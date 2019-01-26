package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.Scheduler;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.subsystems.DriveTrain;
import frc.robot.util.ControlsProcessor;

/*
  The VM is configured to automatically run this class, and to call the
  functions corresponding to each mode, as described in the IterativeRobot
  documentation. If you change the name of this class or the package after
  creating this project, you must also update the manifest file in the resource
  directory.
*/
public class Robot extends TimedRobot {

	// Initialize subsystems
	private DriveTrain drivetrain = new DriveTrain();

	// Initialize auton mode selector
	private Command autonomousCommand;
	private SendableChooser<Command> autoChooser;

	// Initialize robot control systems
	private ControlsProcessor controlsProcessor;

	// Init and Periodic functions
	@Override
	public void robotInit() {
		autoChooser = new SendableChooser<>();
		SmartDashboard.putData("Autonomous Mode Selector", autoChooser);

		// Controls processor only gets created ONCE when code is run
		controlsProcessor = new ControlsProcessor(500000, 1) {
			@Override
			public void registerOperatorControls() {
				append("add_forwards_spline -s 0,0,-6,-6,0,3,6,9,8,8,0,0", this.y);
				append("start_path -s", this.b);
			}
		};

		// Required to register all subsystems in order to be processed. 
		controlsProcessor.registerController("DriveTrain", drivetrain);
		controlsProcessor.start();
	}

	@Override
	public void disabledInit() {
		drivetrain.destruct();
		Scheduler.getInstance().removeAll();

		if (controlsProcessor != null) {
			controlsProcessor.cancelAll();
			controlsProcessor.disable();
		}
	}

	@Override
	public void disabledPeriodic() {
		Scheduler.getInstance().run();
	}

	@Override
	public void autonomousInit() {

		autonomousCommand = autoChooser.getSelected();

		if (autonomousCommand != null) {
			autonomousCommand.start();
		}

		generalInit();

	}

	@Override
	public void autonomousPeriodic() {

		Scheduler.getInstance().run();

	}

	@Override
	public void teleopInit() {
		if (autonomousCommand != null)
			autonomousCommand.cancel();

		generalInit();
	}

	@Override
	public void teleopPeriodic() {

		Scheduler.getInstance().run();

		if(Math.abs(controlsProcessor.getLeftJoystick()) >= 0.1 || Math.abs(controlsProcessor.getRightJoystick()) >= 0.1){
			System.out.println("teleop control");
			drivetrain.arcadeDrive(-controlsProcessor.getLeftJoystick(), controlsProcessor.getRightJoystick());
		}

	}

	@Override
	public void testInit() {

		autonomousCommand = autoChooser.getSelected();

		if (autonomousCommand != null) {
			autonomousCommand.start();
		}

		generalInit();

	}

	@Override
	public void testPeriodic() {

	}

	// General init 
	private void generalInit() {
		if (controlsProcessor != null) {
			controlsProcessor.enable();
		}

		drivetrain.init();
	}
}