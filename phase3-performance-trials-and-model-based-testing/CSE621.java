package CSE621; 

import lejos.hardware.Button;
import lejos.hardware.motor.UnregulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;


public class CSE621 {
	public static void main(String[] args) {
		System.out.println("Starting...");
		Robot robot = Robot.getInstance(State.FORWARD);
		while (true) {
			switch (robot.getState()) {

				case IDLE:
					if (System.currentTimeMillis() - robot.getStartTimeOfCurrentStateMillis() > 1000) {
						robot.setState(State.FORWARD);
					}
					break;

				case FORWARD:
					if (robot.getColor() == Color.RED) {
						robot.setState(State.IDLE);
					}
					if (robot.getColor() == Color.GREEN) {
						robot.setState(State.BACKWARD);
					}
					if (robot.getColor() == Color.YELLOW) {
						robot.setState(State.ROTATE_RIGHT);
					}
					break;

				case BACKWARD:
					if (System.currentTimeMillis() - robot.getStartTimeOfCurrentStateMillis() > 500) {
						robot.setState(State.FORWARD);
					}
					break;

				case ROTATE_RIGHT:
					robot.setState(State.ROTATE_LEFT);
					break;

				case ROTATE_LEFT:
					robot.setState(State.FORWARD);
					break;
			}
		}
	}
}

class Robot {
	private UnregulatedMotor motorLeft;
	private UnregulatedMotor motorRight;
	private EV3UltrasonicSensor ultrasonicSensor;
	private EV3ColorSensor colorSensor;
	private EV3GyroSensor gyroSensor;

	private State state;
	private long startTimeOfCurrentStateMillis;

	private static Robot instance;

	// SINGLETON
	private Robot(State state) {
		this.motorLeft = new UnregulatedMotor(MotorPort.A);
		this.motorRight = new UnregulatedMotor(MotorPort.B);
		this.ultrasonicSensor = new EV3UltrasonicSensor(SensorPort.S1);
		this.colorSensor = new EV3ColorSensor(SensorPort.S2);
		this.gyroSensor = new EV3GyroSensor(SensorPort.S3);

		this.setPower(80);
		this.setState(state);

	}

	public static Robot getInstance(State state) {
		if (instance == null) {
			instance = new Robot(state);
		}
		instance.setState(state);
		return instance;
	}

	// METHODS
	public void goForward() {
		this.motorLeft.forward();
		this.motorRight.forward();
	}

	public void goBackward() {
		this.motorLeft.backward();
		this.motorRight.backward();
	}

	public float getCurrentAngle() {
		this.gyroSensor.setCurrentMode("Angle");
		float[] sample = { 0 };
		this.gyroSensor.fetchSample(sample, 0);
		return sample[0];
	}

	/*
	 * if degree is positive, rotate counterclockwise, else rotate clockwise
	 */
	public void rotateGivenDegrees(float degree) {
		float currentAngle = this.getCurrentAngle();

		if (degree == 0) {
			return;
		} else {
			float targetAngle = currentAngle + degree;
			if (degree > 0) {
				this.rotateCounterClockwise();
				// continue rotating until target angle is achieved
				while (this.getCurrentAngle() < targetAngle) {
				}
			} else {
				this.rotateClockwise();
				// continue rotating until target angle is achieved
				while (this.getCurrentAngle() > targetAngle) {
				}
			}
			this.stop(); // stop the robot after rotation
		}
	}

	public void rotateClockwiseGivenDegrees(float degree) {
		this.rotateGivenDegrees(-degree);
	}

	public void rotateCounterClockwiseGivenDegrees(float degree) {
		this.rotateGivenDegrees(degree);
	}

	public void rotateClockwise() {
		this.motorLeft.forward();
		this.motorRight.backward();
	}

	public void rotateCounterClockwise() {
		this.motorLeft.backward();
		this.motorRight.forward();
	}

	public void stop() {
		this.motorLeft.stop();
		this.motorRight.stop();
	}

	public float getDistanceMetersFromObstacle() {
		// current mode must be set before enabling
		this.ultrasonicSensor.setCurrentMode("Distance");
		this.ultrasonicSensor.enable();

		float[] sample = { 0 };
		this.ultrasonicSensor.getDistanceMode().fetchSample(sample, 0);
		return sample[0];
	}

/*
	public Color getColor() {
		this.colorSensor.setCurrentMode("ColorID");
		int colorId = this.colorSensor.getColorID();

		if (colorId == 0) {
			return Color.RED;
		} else if (colorId == 1) {
			return Color.BLUE;
		} else if (colorId == 2) {
			return Color.GREEN;
		} else if (colorId == 3) {
			return Color.YELLOW;
		} else {
			return Color.UNKNOWN;
		}
	}
*/

	// NOTE:
	// When testing with the papers that were available to us, 
	// the color sensor could differentiate the following colors: 
	// 1. red 
	// 2. green 
	// 3. yellow (actually the paper was orange, but we treated it as yellow as orange color is not recognized in Color enum) 
	// 
	// In particular, the 'blue' colored papers (that were available to us) 
	// were not differentiated by the sensor from 'green' colored paper. 
	// So, we don't expect to have blue-color trigger in the statechart.  


	public Color getColor() {
		this.colorSensor.setCurrentMode("ColorID");
		int colorId = this.colorSensor.getColorID();

		if (colorId == 0) {
			return Color.RED; 
		} else if (colorId == 2) {
			return Color.GREEN;
		} else if (colorId == 3 || colorId == 6) {
			return Color.YELLOW;  // the paper available to us was orangish-yellow (we treat it as yellow)
		} else {
			return Color.UNKNOWN;
		}
	}



	// GETTERS / SETTERS
	public long getStartTimeOfCurrentStateMillis() {
		return this.startTimeOfCurrentStateMillis;
	}

	public State getState() {
		return this.state;
	}

	/**
	 * This method sets the current state of the robot to the passed state, and also
	 * does any actions that need to be done upon entering that state
	 * 
	 * @param state
	 */
	public void setState(State state) {
		this.state = state;
		this.startTimeOfCurrentStateMillis = System.currentTimeMillis();
		System.out.println(state); 
		switch (state) {
		case IDLE:
			this.stop();
			break;
		case FORWARD:
			this.goForward();
			break;
		case BACKWARD:
			this.goBackward();
			break;
		case ROTATE_LEFT:
			this.rotateCounterClockwiseGivenDegrees(90);
			break;
		case ROTATE_RIGHT:
			this.rotateClockwiseGivenDegrees(90);
			break;
		}
	}

	private void setPower(int power) {
		this.motorLeft.setPower(power);
		this.motorRight.setPower(power);
	}

}

enum Color {
	RED, GREEN, BLUE, YELLOW, UNKNOWN
}

enum State {
	IDLE, FORWARD, BACKWARD, ROTATE_LEFT, ROTATE_RIGHT
}