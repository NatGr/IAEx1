package centralized;

import java.util.List;

import logist.simulation.Vehicle;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class Solution {
	// what we call Task in the following is either a task delivery or a task pickup
	private final int[] weights;  // the weight of every Task
	private final City[] cities;  // the city corresponding to every Task
	private final double[] vehicleCapacity;  // initial capacity of every vehicle
	
	// solution characteristics:
	private int[] remainingCapacity;  // the remaining capacity of the vehicle when he accepts the corresponding Task
	private int[] vehicle;  // the offset of the vehicle corresponding the the Task
	private int[] time;  // the time offset of the given Task
	int[] nextTask; // offset of the next Task or -1 if the next Task is null
	
	int nbrVehicles;
	int nbrTasks;
	
	double score;  // score of the current solution
	
	
	public Solution(TaskSet tasks, List<Vehicle> vehicles) {
		nbrVehicles = vehicles.size(); nbrTasks = 2*tasks.size();
		
	}
	
	private boolean checkConstraints() {
		
		return true;
	}
	
	private boolean checkNextEqualsSelf() {
		for (int i=0; i<nbrTasks; i++) {
			if (nextTask[i]==i)
				return false;
		}
		for (int i=nbrTasks; i<nextTask.length;i++) {
			if(nextTask[i]>nbrTasks || nextTask[i]<0) {
				return false;
			}
		}
		return true;
	}
	
	private boolean checkNextVehicleTime() {
		for (int i=0; i<nbrTasks; i++) {
			
		}
		return true;
	}
	
	
}
