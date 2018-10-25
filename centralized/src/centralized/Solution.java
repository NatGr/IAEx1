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
	
	//nextTask(t)!=t
	private boolean checkNextEqualsSelf() {
		//i is index of task
		for (int i=0; i<nbrTasks; i++) {
			if (nextTask[i]==i)
				return false;
		}
		return true;
	}
	
	//nextTask(v_k)=t_j => time(t_j)=1
	private boolean checkNextVehicleTime() {
		//iterate over all vehicles
		for (int i=nbrTasks; i<nextTask.length; i++) {
			//nextTask[i] is the first task of vehicle i-nbrTasks
			if(time[nextTask[i]]!=1) {
				if (nextTask[i]!=-1) { //possibility that a vehicle doesn't execute a task at all
					return false;
				}
			}
		}
		return true;
	}
	
	//nextTask(t_i)=t_j => time(t_j)=time(t_i)+1
	private boolean checkTimeNextTask() {
		for (int i=0; i<nbrTasks; i++) {
			if(nextTask[i]==-1) { //final task so don't need to check time of nexttask
				continue;
			}
			if(time[i]+1!=time[nextTask[i]]) {
				return false;
			}
		}
		return true;
	}
	
	//nextTask(v_k)=t_j => vehicle(t_j)=v_k
	private boolean checkVehicleTaskPair() {
		for (int i=nbrTasks; i<nbrVehicles; i++) {
			if (vehicle[nextTask[i]]!=i) {
				return false;
			}
		}		
		return true;
	}
	
	//nextTask(t_i)=t_j => vehicle(t_j) = vehicle(t_i)
	private boolean checkNextTaskSameVehicle() {
		for (int i=0; i<nbrTasks; i++) {
			if(nextTask[i]==-1) { //final task so don't need to check vehicle of nexttask
				continue;
			}
			if (vehicle[nextTask[i]]!=vehicle[i]) {
				return false;
			}
		}
		
		return true;
	}
	
	//all tasks must be delivered
	private boolean checkAllTasksExecuted() {
		//count occurence of each tasks as next task. Last element is the occurrence of the null element
		int[] countOccurence = new int[nbrTasks + 1];
		for (int i=0; i<nextTask.length; i++) {
			if (nextTask[i]==-1) {
				countOccurence[countOccurence.length-1]++;
			}
			else {
				countOccurence[nextTask[i]]++;
			}
		}
		for (int i=0; i<countOccurence.length-1;i++) {
			if (countOccurence[i]!=1) {
				return false;
			}
		}
		if(countOccurence[countOccurence.length-1]!=nbrVehicles) {
			return false;
		}
		return true;
	}
	
	
	//The capacity of a vehicle cannot be exceeded
	private boolean checkCapacity() {
		for (int i=0; i<weights.length; i++) {
			if (weights[i]>vehicleCapacity[vehicle[i]]) {
				return false;
			}				
		}
		return true;
	}
	
	
}
