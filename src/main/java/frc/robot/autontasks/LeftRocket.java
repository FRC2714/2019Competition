package frc.robot.autontasks;

import frc.robot.util.AutonTask;
import frc.robot.util.ControlsProcessor;

public class LeftRocket extends AutonTask {
	/**
	 * Accepts the object of the running controlsProcessor to modify
	 *
	 * @param controlsProcessor
	 */
	public LeftRocket(ControlsProcessor controlsProcessor) {
		super(controlsProcessor);


		queueTask("set_angular_offset -s -180");
		queueTask("hatch_intake -p");
		queueTask("hatch_true -p");

		queueTask("start_path -s");
		queueTask("delayed_to_position -p 5,100,1.5");

		queueTask("turn_to_angle_setpoint -s 300");

		queueTask("auton_vision_align -s 4");
		queueTask("upper_score -p");

		queueTask("extake -s");
		queueTask("add_backwards_line -p 7.2,21.4,4.2,24.4,5,5,0,0");

		queueTask("start_path -s");

		queueTask("turn_to_angle_setpoint -s 270");
		queueTask("add_forwards_spline -p 4.2,24.4,270,1,6.5,5,270,6,7,12,0,0");

		queueTask("set_current_position -s 4.2,24.4");

		queueTask("start_path -s");

		queueTask("delayed_to_position -p 0.8,86,2");

//		queueTask("turn_to_angle_setpoint -s 270")

		queueTask("hatch_station_intake -s");
		queueTask("auton_vision_align -p 2.4");  // Old: 3.5
	}
}
