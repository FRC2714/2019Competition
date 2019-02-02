package frc.robot.subsystems;

import com.kauailabs.navx.frc.AHRS;
import com.revrobotics.CANEncoder;
import com.revrobotics.CANPIDController;
import com.revrobotics.CANSparkMax;
import com.revrobotics.ControlType;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.wpilibj.CounterBase.EncodingType;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import frc.robot.RobotMap;
import frc.robot.util.ControlsProcessor;
import frc.robot.util.DrivingController;
import frc.robot.util.Odometer;
import frc.robot.util.SubsystemCommand;
import frc.robot.util.SubsystemModule;

public class DriveTrain extends SubsystemModule {

	// Drivetrain motors
	private CANSparkMax lMotor0 = new CANSparkMax(1, MotorType.kBrushless);
	private CANSparkMax lMotor1 = new CANSparkMax(2, MotorType.kBrushless);
	private CANSparkMax lMotor2 = new CANSparkMax(3, MotorType.kBrushless);
	private CANSparkMax rMotor0 = new CANSparkMax(4, MotorType.kBrushless);
	private CANSparkMax rMotor1 = new CANSparkMax(5, MotorType.kBrushless);
	private CANSparkMax rMotor2 = new CANSparkMax(6, MotorType.kBrushless);

	// PID controllers
	private CANPIDController lPidController = lMotor0.getPIDController();
	private CANPIDController rPidController = rMotor0.getPIDController();

	// MAX encoders
	private CANEncoder lEncoder = lMotor0.getEncoder();
	private CANEncoder rEncoder = rMotor0.getEncoder();

	// Differential drivetrain
	private DifferentialDrive drive = new DifferentialDrive(lMotor0, rMotor0);

	// PID coefficients
	private final double kMinOutput = -1;
	private final double kMaxOutput = 1;

	private final double kP = 4.8e-5;
	private final double kI = 5.0e-7;
	private final double kD = 0.0;
	private final double kIS = 0.0;

	private final double lKFF = 1.77e-4;
	private final double rKFF = 1.78e-4;

	private final double rpmToFeet = 0.003135; // Convert RPM to ft/s
	private final double rotationsToFeet = 0.1881; // Convert rotations to feet

	private double startTime;
	private int numberOfRuns;


	private final double sensitivity = 2.5;
	private final double maxVelocity = 13;
	private final double maxAcceleration = 30;

	// 
	private double leftEncoderOffset = 0;
	private double rightEncoderOffset = 0;
	
	private double lastVelocity = 0;

	// Robot characteristics
	private double wheelSeparation = 2;

	private boolean driverControlled = false;

	private ControlsProcessor controlsProcessor;

	// Gearbox encoders
	//private Encoder leftEncoder = new Encoder(RobotMap.p_leftEncoderA, RobotMap.p_leftEncoderB, true, EncodingType.k4X);
	//private Encoder rightEncoder = new Encoder(RobotMap.p_rightEncoderA, RobotMap.p_rightEncoderB, true,
	//		EncodingType.k4X);

	// NavX gyro
	private AHRS navX = new AHRS(SPI.Port.kMXP);

	// Drivetrain initializations
	public DriveTrain(ControlsProcessor controlsProcessor) {
		registerCommands();

		this.controlsProcessor = controlsProcessor;

		drive.setSafetyEnabled(false);
		// Configure follow mode
		lMotor1.follow(lMotor0);
		lMotor2.follow(lMotor0);
		rMotor1.follow(rMotor0);
		rMotor2.follow(rMotor0);

		// Setup up PID coefficients
		lPidController.setP(kP);
		lPidController.setI(kI);
		lPidController.setD(kD);
		lPidController.setIZone(kIS);
		lPidController.setFF(lKFF);
		lPidController.setOutputRange(kMinOutput, kMaxOutput);

		rPidController.setP(kP);
		rPidController.setI(kI);
		rPidController.setD(kD);
		rPidController.setIZone(kIS);
		rPidController.setFF(rKFF);
		rPidController.setOutputRange(kMinOutput, kMaxOutput);
	}

	// Instantiate odometer and link in encoders and navX
	public Odometer odometer = new Odometer(0,0,0) {

		@Override
		public void updateEncodersAndHeading() {
			this.headingAngle = 450 - navX.getFusedHeading();
			if(this.headingAngle > 360) {
				this.headingAngle -= 360;
			}	

			this.leftPos = lEncoder.getPosition() * rotationsToFeet;
			this.rightPos = -rEncoder.getPosition() * rotationsToFeet;
			
			double leftVelocity = lEncoder.getVelocity() * rpmToFeet;
			double rightVelocity = -rEncoder.getVelocity() * rpmToFeet;

			this.currentAverageVelocity = (leftVelocity + rightVelocity) / 2;	
		}
	};

	// Instantiate point controller for autonomous driving
	public DrivingController drivingcontroller = new DrivingController(0.005) {

		/**
		 * Use output from odometer and pass into autonomous driving controller
		 */
		@Override
		public void updateVariables(){
			this.currentX = odometer.getCurrentX();
			this.currentY = odometer.getCurrentY();
			this.currentAngle = odometer.getHeadingAngle();
			this.currentAverageVelocity = odometer.getCurrentAverageVelocity();
		}

		/**
		 * Link autonomous driving controller to the drive train motor control
		 */
		@Override
		public void driveRobot(double power, double pivot) {
			closedLoopArcade(power, pivot);
		}
	};

	/**
	 * Resets the variables for the drivetrain
	 */
	@Override
	public void init() {
		leftEncoderOffset = lEncoder.getPosition();
		rightEncoderOffset = -rEncoder.getPosition();
		navX.reset();
		navX.zeroYaw();

		// leftEncoder.setDistancePerPulse(-0.0495);
		// rightEncoder.setDistancePerPulse(0.00105);

		lMotor0.setIdleMode(CANSparkMax.IdleMode.kCoast);
		rMotor0.setIdleMode(CANSparkMax.IdleMode.kCoast);
	}

	/**
	 * Disables the motors and stops the drivetrain
	 */
	@Override
	public void destruct() {
		driverControlled = false;

		lMotor0.setIdleMode(CANSparkMax.IdleMode.kBrake);
		rMotor0.setIdleMode(CANSparkMax.IdleMode.kBrake);

		lMotor0.set(0);
		rMotor0.set(0);

		disable();
		drivingcontroller.clearControlPath();
	}
	
	/**
	 * Subsystem run function, uses ControlsProcessor (multi-threaded at fast period)
	 */
	@Override
	public void run() {

		// Run every time
		this.odometer.integratePosition();

		// Run only when subsystem is enabled
		if (getStatus()) {
			this.drivingcontroller.run();
		}
	}

	// General arcade drive
	public void arcadeDrive(double power, double pivot) {
		drive.arcadeDrive(power, pivot);
	}

	// Closed loop velocity based tank without an acceleration limit
	public void closedLoopTank(double leftVelocity, double rightVelocity) {
		lPidController.setReference(leftVelocity / rpmToFeet, ControlType.kVelocity);
		rPidController.setReference(-rightVelocity / rpmToFeet, ControlType.kVelocity);
		//System.out.println("ls: " + leftVelocity / rpmToFeet + " rs: " + -rightVelocity / rpmToFeet);
	}

	// Closed loop arcade based tank
	public void closedLoopArcade(double velocity, double pivot) {
		pivot = pivot * sensitivity;
		closedLoopTank(velocity - pivot, velocity + pivot);
		//System.out.println("pivot " + pivot);
	}

	// Closed loop velocity based tank with an acceleration limit
	public void closedLoopArcade(double velocity, double pivot, double accelLimit) {
		accelLimit *= controlsProcessor.getCommandPeriod();

		double velocitySetpoint = velocity;

		if (Math.abs(velocity - lastVelocity) > accelLimit) {
			if (velocity - lastVelocity > 0)
				velocitySetpoint = lastVelocity + accelLimit;
			else
				velocitySetpoint = lastVelocity - accelLimit;
		}

		closedLoopArcade(velocitySetpoint, pivot);

		lastVelocity = velocitySetpoint;
	}

	// Output encoder values
	public void getEncoderValues() {
		System.out.println("LE: " + lEncoder.getPosition() + " RE: " + rEncoder.getPosition());
	}

	public double getMaxVelocity(){
		return maxVelocity;
	}

	@Override
	public void registerCommands() {
		new SubsystemCommand(this.registeredCommands, "driver_control") {

			@Override
			public void initialize() {
				driverControlled = true;
			}

			@Override
			public void execute() {
				double power = 0;
				double pivot = 0;

				if (Math.abs(controlsProcessor.getLeftJoystick()) > .1)	
					power = controlsProcessor.getLeftJoystick();
				if (Math.abs(controlsProcessor.getRightJoystick()) > .1)
					pivot = controlsProcessor.getRightJoystick();

				closedLoopArcade(-power * maxVelocity, -pivot, maxAcceleration);
			}

			@Override
			public boolean isFinished() {
				return !driverControlled;
			}

			@Override
			public void end() {
				closedLoopArcade(0, 0);
			}
		};

		new SubsystemCommand(this.registeredCommands, "closed_loop_tank") {

			@Override
			public void initialize() {
				driverControlled = false;

				double velocity = Double.parseDouble(this.args[0]);
				closedLoopTank(velocity, velocity);
			}

			@Override
			public void execute() {

			}

			@Override
			public boolean isFinished() {
				return false;
			}

			@Override
			public void end() {
				closedLoopTank(0, 0);
			}
		};

		new SubsystemCommand(this.registeredCommands, "set_angular_offset") {

			@Override
			public void initialize() {
				odometer.setOffset(Double.parseDouble(this.args[0]));
				navX.zeroYaw();
			}

			@Override
			public void execute() {

			}

			@Override
			public boolean isFinished() {
				return true;
			}

			@Override
			public void end() {

			}
		};

		new SubsystemCommand(this.registeredCommands, "add_forwards_spline") {

			@Override
			public void initialize() {
				drivingcontroller.addSpline(Double.parseDouble(this.args[0]), Double.parseDouble(this.args[1]),
						Double.parseDouble(this.args[2]), Double.parseDouble(this.args[3]),
						Double.parseDouble(this.args[4]), Double.parseDouble(this.args[5]),
						Double.parseDouble(this.args[6]), Double.parseDouble(this.args[7]),
						Double.parseDouble(this.args[8]), Double.parseDouble(this.args[9]),
						Double.parseDouble(this.args[10]), Double.parseDouble(this.args[11]), true);
			}

			@Override
			public void execute() {

			}

			@Override
			public boolean isFinished() {
				return true;
			}

			@Override
			public void end() {
				
			}
		};

		new SubsystemCommand(this.registeredCommands, "add_backwards_spline") {

			@Override
			public void initialize() {
				drivingcontroller.addSpline(Double.parseDouble(this.args[0]), Double.parseDouble(this.args[1]),
						Double.parseDouble(this.args[2]), Double.parseDouble(this.args[3]),
						Double.parseDouble(this.args[4]), Double.parseDouble(this.args[5]),
						Double.parseDouble(this.args[6]), Double.parseDouble(this.args[7]),
						Double.parseDouble(this.args[8]), Double.parseDouble(this.args[9]),
						Double.parseDouble(this.args[10]), Double.parseDouble(this.args[11]), false);
			}

			@Override
			public void execute() {

			}

			@Override
			public boolean isFinished() {
				return true;
			}

			@Override
			public void end() {

			}
		};

		

		new SubsystemCommand(this.registeredCommands, "start_path") {

			@Override
			public void initialize() {
				enable();
			}

			@Override
			public void execute() {
			}

			@Override
			public boolean isFinished() {
				return drivingcontroller.isFinished();
			}

			@Override
			public void end() {
				closedLoopArcade(0, 0);
				disable();
			}
		};

		new SubsystemCommand(this.registeredCommands, "delay_tester"){
			@Override
			public void initialize() {
//				System.out.println("Delay = " + Double.parseDouble(this.args[1]));
//				enable();
			}

			@Override
			public void execute() {
				System.out.println("DELAY TESTER running!" );
			}

			@Override
			public boolean isFinished() {
				return false;
			}

			@Override
			public void end() {
			}
		};


	}

}