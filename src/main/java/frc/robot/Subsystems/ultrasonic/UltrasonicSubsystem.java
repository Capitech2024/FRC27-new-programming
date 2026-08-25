package frc.robot.Subsystems.ultrasonic;

import edu.wpi.first.wpilibj.Ultrasonic;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.math.filter.MedianFilter;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class UltrasonicSubsystem extends SubsystemBase {

    private static final int PING_CHANNEL = 0;  // Trigger
    private static final int ECHO_CHANNEL = 1;  // Echo

    private final Ultrasonic ultrasonic;
    private final MedianFilter filter = new MedianFilter(5);

    public UltrasonicSubsystem() {
        ultrasonic = new Ultrasonic(PING_CHANNEL, ECHO_CHANNEL);

        ultrasonic.setAutomaticMode(true);
    }

    public double getDistanceInches() {
        double raw = ultrasonic.getRangeInches();
        return filter.calculate(raw);
    }

    public double getDistanceMM() {
        return getDistanceInches() * 25.4;
    }

    public boolean isRangeValid() {
        return ultrasonic.isRangeValid();
    }

    @Override
    public void periodic() {
        if (isRangeValid()) {
            SmartDashboard.putNumber("Ultrasonic Distance (in)", getDistanceInches());
        } else {
            SmartDashboard.putNumber("Ultrasonic Distance (in)", -1);
        }

        SmartDashboard.putBoolean("Ultrasonic Valid", isRangeValid());
    }
}