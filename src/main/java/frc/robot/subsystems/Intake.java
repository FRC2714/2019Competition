package frc.robot.subsystems;

import java.util.ArrayList;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import com.revrobotics.CANSparkMaxLowLevel.PeriodicFrame;

import edu.wpi.first.wpilibj.Servo;
import edu.wpi.first.wpilibj.Spark;
import frc.robot.util.SubsystemCommand;
import frc.robot.util.SubsystemModule;

public class Intake extends SubsystemModule {

	// Intake Motors
	protected CANSparkMax cargoMotor = new CANSparkMax(9, MotorType.kBrushless);
	protected CANSparkMax pumpMotor = new CANSparkMax(10, MotorType.kBrushless);

	// Blinkin
	private Spark blinkin = new Spark(0);

	// Hatchplate Servo
	private Servo valveServo = new Servo(1);

	// Maximum currents for cargo and hatch intakes
	private final double cargoCurrentThreshold = 30;
	private final double pumpCurrentDiffrence = 1; // 3.125 for two

	// Pump state values
	private double pumpStateFirstAvg;
	private boolean pumpStateIsFirstAvg;
	private int pumpStateCounter;

	// ArrayList holding the read currents
	private ArrayList<Double> cargoCurrents;
	private ArrayList<Double> pumpCurrents;

	// Average current
	private double cargoAverageCurrent;
	private double pumpAverageCurrent;

	// Number of current values stored
	private final int numberOfCargoCurrents = 50;
	private final int numberOfPumpCurrents = 100;

	// Intake States - Public so Arm can access the states for state-based logic
	private boolean cargoState;
	private boolean pumpState;

	public Intake() {
		registerCommands(); // Puts commands onto the hashmap

		cargoMotor.setPeriodicFramePeriod(PeriodicFrame.kStatus0, 5);
		cargoMotor.setPeriodicFramePeriod(PeriodicFrame.kStatus1, 5);

		pumpMotor.setPeriodicFramePeriod(PeriodicFrame.kStatus0, 5);
		pumpMotor.setPeriodicFramePeriod(PeriodicFrame.kStatus1, 5);
	}

	/**
	 * Puts pump servo into hatch intake mode
	 */
	public void pumpHatch() {
		valveServo.set(0);
	}

	/**
	 * Puts pump servo into release mode
	 */
	public void pumpRelease() {
		valveServo.set(1);
	}

	/**
	 * Uses the roller's current draw to determine if the robot has a cargo
	 * @return Cargo = true or
	 */
	public boolean checkCargoState() {
		if(cargoMotor.get() > 0.5) {
			cargoCurrents.add(0, Math.abs(cargoMotor.getOutputCurrent()));

			if(cargoCurrents.size() != numberOfCargoCurrents) {
				cargoAverageCurrent += cargoCurrents.get(0) / numberOfCargoCurrents;

				return cargoState;
			}
			else {
				cargoAverageCurrent += (cargoCurrents.get(0) - cargoCurrents.get(cargoCurrents.size() - 1)) / numberOfCargoCurrents;
				cargoCurrents.remove(cargoCurrents.size() - 1);

				return cargoAverageCurrent > cargoCurrentThreshold;
			}
		}

		cargoCurrents = new ArrayList<Double>(0);
		cargoAverageCurrent = 0;

		return cargoState;
	}

	public void clearStates() {
		cargoState = false;
		pumpState = false;
	}

	public boolean checkPumpState() {
		if(pumpState) { return pumpState; }
		if(pumpMotor.get() > 0) {
			pumpCurrents.add(0, Math.abs(pumpMotor.getOutputCurrent()));

			if(pumpCurrents.size() != numberOfPumpCurrents) {
				pumpAverageCurrent += pumpCurrents.get(0) / numberOfPumpCurrents;
				pumpStateCounter = 0;
				return pumpState;
			}
			else {
				if(pumpStateCounter > 10 && pumpStateIsFirstAvg) {
					pumpStateFirstAvg = pumpAverageCurrent;
					pumpStateIsFirstAvg = false;
				}

				pumpStateCounter++;

				pumpAverageCurrent += (pumpCurrents.get(0) - pumpCurrents.get(pumpCurrents.size() - 1)) / numberOfPumpCurrents;
				pumpCurrents.remove(pumpCurrents.size() - 1);

				if(Math.abs(pumpAverageCurrent) < Math.abs(pumpStateFirstAvg) - pumpCurrentDiffrence) {
					return true;
				} else {
					return false;
				}
			}
		}

		pumpCurrents = new ArrayList<Double>(0);
		pumpAverageCurrent = 0;
		pumpStateFirstAvg = 0;
		pumpStateCounter = 0;
		pumpStateIsFirstAvg = true;

		return pumpState;
	}

	public boolean getCargoState() {
		return cargoState;
	}

	public boolean getHatchState() {
		return pumpState;
	}

	@Override
	public void run() {
		cargoState = checkCargoState();
		pumpState = checkPumpState();

		if(cargoState || pumpState) { blinkin.set(0.65); }
		else { blinkin.set(0.99); }

		// if(cargoCurrents.size() >= numberOfCargoCurrents || hatchCurrents.size() >= numberOfHatchCurrents || pumpCurrents.size() >= numberOfPumpCurrents)
		// 	System.out.println("Cargo Current: " + cargoAverageCurrent + "\tHatch Current: " + hatchAverageCurrent + "\tPump Current: " + pumpAverageCurrent);
	}

	@Override
	public void registerCommands() {

		new SubsystemCommand(this.registeredCommands, "cargo_true") {

			@Override
			public void initialize() {
				cargoState = true;
				pumpState = false;

				// System.out.println("Override cargo");
			}

			@Override
			public void execute() {}

			@Override
			public boolean isFinished() {
				return true;
			}

			@Override
			public void end() {}
		};

		new SubsystemCommand(this.registeredCommands, "hatch_true") {

			@Override
			public void initialize() {
				cargoState = false;
				pumpState = true;

				// System.out.println("Override hatch station");
			}

			@Override
			public void execute() {}

			@Override
			public boolean isFinished() {
				return true;
			}

			@Override
			public void end() {}
		};

		new SubsystemCommand(this.registeredCommands, "intake_stop") {

			@Override
			public void initialize() {
				cargoMotor.set(0);
				pumpMotor.set(0);

				cargoState = false;
				pumpState = false;
			}

			@Override
			public void execute() {
				pumpRelease();
			}

			@Override
			public boolean isFinished() {
				return false;
			}

			@Override
			public void end() {}
		};


		new SubsystemCommand(this.registeredCommands, "cargo_intake") {

			@Override
			public void initialize() {
				if(!pumpState) {
					cargoMotor.set(0.75);
					pumpMotor.set(0);
				}
			}

			@Override
			public void execute() {}

			@Override
			public boolean isFinished() {
				return cargoState || pumpState;
			}

			@Override
			public void end() {
				if (cargoState) {
					cargoMotor.set(0.15);
				} else {
					cargoMotor.set(0);
				}
			}
		};

		new SubsystemCommand(this.registeredCommands, "hatch_floor_intake") {

			@Override
			public void initialize() {
				if(!cargoState && !pumpState) {
					cargoMotor.set(-0.75);
					pumpMotor.set(1);
				}
			}

			@Override
			public void execute() {
				pumpHatch();
			}

			@Override
			public boolean isFinished() {
				return cargoState || pumpState;
			}

			@Override
			public void end() {
				if(!cargoState) { cargoMotor.set(0); }
			}
		};

		new SubsystemCommand(this.registeredCommands, "hatch_station_intake") {

			@Override
			public void initialize() {
				if(!cargoState) {
					pumpMotor.set(1);
				}
			}

			@Override
			public void execute() {
				pumpHatch();
			}

			@Override
			public boolean isFinished() {
				return cargoState || pumpState;
			}

			@Override
			public void end() { }
		};

		new SubsystemCommand(this.registeredCommands, "set_cargo_mode") {

			@Override
			public void initialize() {
				cargoState = true;
				pumpState = false;
			}

			@Override
			public void execute() {}

			@Override
			public boolean isFinished() {
				return true;
			}

			@Override
			public void end() {}
		};

		new SubsystemCommand(this.registeredCommands, "set_hatch_mode") {

			@Override
			public void initialize() {
				cargoState = false;
				pumpState = true;
			}

			@Override
			public void execute() {}

			@Override
			public boolean isFinished() {
				return true;
			}

			@Override
			public void end() {}
		};

		new SubsystemCommand(this.registeredCommands, "hatch_intake") {
			boolean intaking;

			@Override
			public void initialize() {
				intaking = false;
			}

			@Override
			public void execute() {
				pumpHatch();

				if(!intaking) {
					pumpMotor.set(1);
					intaking = true;
				}
			}

			@Override
			public boolean isFinished() {
				return cargoState || pumpState;
			}

			@Override
			public void end() {}
		};

		new SubsystemCommand(this.registeredCommands, "climber_pump") {

			@Override
			public void initialize() {
				pumpMotor.set(-1.0);
			}

			@Override
			public void execute() {
				pumpHatch();
			}

			@Override
			public boolean isFinished() {
				return true;
			}

			@Override
			public void end() {}
		};
	}

	@Override
	public void init() {
		cargoMotor.setIdleMode(CANSparkMax.IdleMode.kBrake);
		pumpMotor.setIdleMode(CANSparkMax.IdleMode.kBrake);

		cargoMotor.set(0);
		pumpMotor.set(0);

		cargoCurrents = new ArrayList<Double>(0);
		pumpCurrents = new ArrayList<Double>(0);

		cargoAverageCurrent = 0;
		pumpAverageCurrent = 0;

		cargoState = false;
		pumpState = false;

		pumpStateIsFirstAvg = true;

		blinkin.set(0.99);
	}

	@Override
	public void destruct() {
		cargoMotor.setIdleMode(CANSparkMax.IdleMode.kBrake);
		pumpMotor.setIdleMode(CANSparkMax.IdleMode.kBrake);

		cargoMotor.set(0);
		pumpMotor.set(0);

		blinkin.set(-0.57);
	}
}
