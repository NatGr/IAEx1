package deliberative;

import java.util.ArrayList;
import java.util.Arrays;

import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class State implements Cloneable, Comparable<State> {
	Task[] availableTasks; // much less memory intensive than an arraylist
	Task[] pickedUpTasks; // we use arrays of size 0 instead of null when the array is empty
	State parent;
	City city;
	int remainingCapacity;
	int cost = 0; //for A-start algorithm
	
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
		// rest is set to null
	}
	
	// returns an arraylist containing the children of the state
	public ArrayList<State> createChildren() {
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
					//We calculate the total cost for delivery when picking up the package
					child.cost = (int) (parent.cost + availableTasks[i].pickupCity.distanceTo(availableTasks[i].deliveryCity) - availableTasks[i].reward); //TODO: Still need to multiply this with cost per km
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
				children.add(child);
			}
			return children;
			
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			System.exit(-1); // TODO: Thibauld, I have no idea if that's the "java good practice way to handle this", feel free to change
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

	@Override
	public int compareTo(State s2) {
		//System.out.println("Compare agent 1: ("+this.cost+") with agent 2: ("+s2.cost+")");
		return this.cost - s2.cost;
	}

}
