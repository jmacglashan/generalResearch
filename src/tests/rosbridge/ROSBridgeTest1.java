package tests.rosbridge;

import edu.wpi.rail.jrosbridge.Ros;
import edu.wpi.rail.jrosbridge.Topic;
import edu.wpi.rail.jrosbridge.callback.TopicCallback;
import edu.wpi.rail.jrosbridge.messages.Message;


public class ROSBridgeTest1 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Ros ros = new Ros("192.168.160.185");
		ros.connect();
		
		System.out.println("Finished connect");
		
		/*
		Topic echoBack = new Topic(ros, "/camera/rgb/image_raw", "std_msgs/String");
			echoBack.subscribe(new TopicCallback() {
		        @Override
		        public void handleMessage(Message message) {
		            System.out.println("From ROS: " + message.toString());
		        }
		    });
			
		*/
		


	}

}
