package centralized;

import java.util.List;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class Solution {
	// what we call Task in the following is either a task delivery or a task pickup
	private final int[] weights;  // the weight of every Task
	private final City[] cities;  // the city corresponding to every Task and every Vehicle (initial city)
	// that array is orderer by : [Task_0 (pickup), Task_0 (delivery), ..., Task_n (delivery), Vehicle_m, ..., Vehicle_0]
	private final double[] vehicleCapacity;  // initial capacity of every vehicle
	private final int nbrVehicles, nbrTasks;
	
	// solution characteristics:
	private int[] remainingCapacity;  // the remaining capacity of the vehicle when he accepts the corresponding Task
	private int[] vehicle;  // the offset of the vehicle corresponding the the Task
	private int[] time;  // the time offset of the given Task
	int[] nextTask; // offset of the next Task or -1 if the next Task is null
	// that array is orderer by : [Task_0 (pickup), Task_0 (delivery), ..., Task_n (delivery), Vehicle_m, ..., Vehicle_0]
	double score;  // score of the current solution
	
	public Solution(Task[] tasks, List<Vehicle> vehicles) {
		nbrVehicles = vehicles.size();
		nbrTasks = 2*tasks.length;
		
	}
	
	private boolean checkConstraints() {
		return true;
	}
}
