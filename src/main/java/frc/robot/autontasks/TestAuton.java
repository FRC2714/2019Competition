package frc.robot.autontasks;

import frc.robot.util.AutonTask;
import frc.robot.util.ControlsProcessor;

public class TestAuton extends AutonTask {

    public TestAuton(ControlsProcessor controlsProcessor) {
        super(controlsProcessor);
        
        // queueTask("add_forwards_spline -s 0,0,0,0,0,0,0,10,5,5,0,0");
        queueTask("set_angular_offset -s -180");
        queueTask("add_backwards_spline -s 0,0,5.95,7.25,0,7,7.95,10.45,13,13,0,0");
        queueTask("add_forwards_spline -s 7.25,5.95,7,7,10.45,7.95,0,-3,13,13,0,0");
        queueTask("add_backwards_spline -s 7,7,5.8,5.8,-3,0,11,13.5,13,13,0,8");
        queueTask("add_backwards_spline -s 5.8,5.8,5.8,4.8,13.5,15,18,19.5,13,8,8,0");
        queueTask("add_forwards_spline -s 4.8,4.8,4.8,7,19.5,19.5,19.5,17,13,8,0,0");
        queueTask("start_path -s");
    }
}