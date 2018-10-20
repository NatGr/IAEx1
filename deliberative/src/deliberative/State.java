package deliberative;

import java.util.ArrayList;
import java.util.Arrays;

import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class State implements Cloneable, Comparable<State> {
	private Task[] availableTasks; // much less memory intensive than an arraylist
	private Task[] pickedUpTasks; // we use arrays of size 0 instead of null when the array is empty
	State parent;
	private City city;
	private int remainingCapacity;
	double cost; // cost of the state, simply the total distance from base state
	double heuristic; // cost of the heuristic
	
	/* constructor takes the current city of the vehicle, the set of the available tasks,
	 *  the set of the picked up tasks and the capacity of the vehicle
	 *  VehicleCapacity is assumed to be bigger than the sum of the weight of the pickedUpTasks */
	public State(City city, TaskSet availableTasks, TaskSet pickedUpTasks, int VehicleCapacity) {
		this.city = city;
		remainingCapacity = VehicleCapacity;
		if (availableTasks != null) {
			this.availableTasks = new Task[availableTasks.size()];
			int i = 0;
			for (Task task: availableTasks) {
				this.availableTasks[i] = task;
				i++;
			}
		}
		if (pickedUpTasks != null) {
			this.pickedUpTasks = new Task[pickedUpTasks.size()];
			int i = 0;
			for (Task task: pickedUpTasks) {
				this.pickedUpTasks[i] = task;
				i++;
				remainingCapacity -= task.weight;
			}
		}
		// rest is set to null or to 0 (java default initialisation)
	}
	
	// returns an arraylist containing the children of the state
	// the useHeuristic arguments specifies wether we should update the heuristic field of the childrens
	public ArrayList<State> createChildren(boolean useHeuristic) {
		try {
			if (isTerminal()) {
				return null;
			}
			ArrayList<State> children = new ArrayList<State>();
			// 1. take a new package
			for (int i = 0; i < availableTasks.length; i++) {
				if (remainingCapacity >= availableTasks[i].weight) {
					State child = (State) this.clone();
					child.parent = this;
					
					child.availableTasks = new Task[availableTasks.length - 1]; // might be an array of size 0
					for (int j = 0; j < i; j++) {
						child.availableTasks[j] = availableTasks[j];
					}
					for (int j = i+1; j < availableTasks.length; j++) {
						child.availableTasks[j-1] = availableTasks[j];
					}
					
					// we have to add it to the task to the picked up tasks
					child.pickedUpTasks = Arrays.copyOf(pickedUpTasks, pickedUpTasks.length + 1);
					child.pickedUpTasks[pickedUpTasks.length] = availableTasks[i];
					child.city = availableTasks[i].pickupCity;
					child.remainingCapacity -= availableTasks[i].weight;
					child.cost += this.city.distanceTo(child.city); // cost to go and pickup that package
					if (useHeuristic) {
						child.computeHeuristic();
					}
					children.add(child);
				}
			}
			
			// 2. deliver one of the packages we currently have
			for (int i = 0; i < pickedUpTasks.length; i++) {
				State child = (State) this.clone();
				child.parent = this;
				
				child.pickedUpTasks = new Task[pickedUpTasks.length - 1];
				for (int j = 0; j < i; j++) {
					child.pickedUpTasks[j] = pickedUpTasks[j];
				}
				for (int j = i+1; j < pickedUpTasks.length; j++) {
					child.pickedUpTasks[j-1] = pickedUpTasks[j];
				}
				
				child.city = pickedUpTasks[i].deliveryCity;
				child.remainingCapacity += pickedUpTasks[i].weight;
				child.cost += this.city.distanceTo(child.city); // cost to go and deliver that package
				if (useHeuristic) {
					child.computeHeuristic();
				}
				children.add(child);
			}
			return children;
			
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}
	
	/* return the Plan to get to the last state */
	public Plan getPlan() {
		if (parent == null) {
			return new Plan(city);
		} else {
			Plan plan = parent.getPlan();
			/*
			 * There has to be at least one difference with the parent state
			 * Either the availableTasks length is one shorter
			 * Or either the pickedUpTasks length is one shorter*/
			// 1. we took take a new package
			if (availableTasks.length < parent.availableTasks.length) {
				for (int i = 0; i < parent.availableTasks.length; i++) {
					if (i == availableTasks.length || availableTasks[i] != parent.availableTasks[i]) {
						// if i == availableTasks.length, it means parent took its last available task
						// if availableTasks[i] != parent.availableTasks[i], it means pakent took action number i
						for (City city : parent.city.pathTo(parent.availableTasks[i].pickupCity)) {
							plan.appendMove(city);
						}
						plan.appendPickup(parent.availableTasks[i]);
						break; // this should not trigger more than once
					}
				}
			} else { // 2. we delivered one of the packages we had
				for (int i = 0; i < parent.pickedUpTasks.length; i++) {
					if (i == pickedUpTasks.length || pickedUpTasks[i] != parent.pickedUpTasks[i]) {
						for (City city : parent.city.pathTo(parent.pickedUpTasks[i].deliveryCity)) {
							plan.appendMove(city);	
						}
						plan.appendDelivery(parent.pickedUpTasks[i]);
						break;
					}
				}
			}
			
			return plan;
		}
	}
	
	// return wether the state is terminal (i.e., if all packages were delivered)
	public boolean isTerminal() {
		return availableTasks.length == 0 && pickedUpTasks.length == 0;
	}

	/* return the difference between this state's cost and s2's cost, including the heuristic 
	(if no heuristic is used, heuristic = 0), the difference is rounded to -1, 0 or +1 */
	@Override
	public int compareTo(State s2) {
		double diff = this.cost + this.heuristic - s2.cost - s2.heuristic;
		if (diff < 0) {
			return -1;
		} else if (diff > 0) {
			return 1;
		} else {
			return 0;
		}
	}
	
	// returns true if both states are equivalent, this does not take the cost and the parent into account!
	public boolean equals(Object obj) {
		if (obj == null || ! (obj instanceof State)) {
			return false;
		}
		State s2 = (State) obj;
		if (city != s2.city || availableTasks.length != s2.availableTasks.length || pickedUpTasks.length != s2.pickedUpTasks.length) {
			return false;
		} else {
			for (int i = 0; i < availableTasks.length; i++) {
				if (availableTasks[i] != s2.availableTasks[i]) { // pointer comparison is good enough, 
					// the tasks are always ordered in the same way as the initialState
					return false;
				}
			}
			for (int i = 0; i < pickedUpTasks.length; i++) {
				if (pickedUpTasks[i] != s2.pickedUpTasks[i]) {
					return false;
				}
			}
			return true;
		}
	}
	
	/* computes the heuristic and updates the heuristic field
	 * the heuristic used here is the maximum over all packages of the distance we will need to deliver that package from 
	 * where we are (ignoring vehicle capacity and all of the other packages
	 */
	private void computeHeuristic() {
		heuristic = 0;
		for (Task task: availableTasks) {
			if (city != task.pickupCity) {
				heuristic = Math.max(heuristic, city.distanceTo(task.pickupCity) + task.pathLength());
			} else {
				heuristic = Math.max(heuristic, task.pathLength());
			}
		}
		for (Task task: pickedUpTasks) {
			heuristic = Math.max(heuristic, city.distanceTo(task.deliveryCity));
		}
	}
	
	// return a hashcode for the state
	public int hashCode() {
		final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(availableTasks);
        result = prime * result + Arrays.hashCode(pickedUpTasks);
        result = prime * result + city.hashCode();
        return result;
	}
	
	// city getter
	public City getCity() {
		return this.city;
	}
	
	// availableTasks getter
	public Task[] getAvailableTasks() {
		return this.availableTasks;
	}
}
