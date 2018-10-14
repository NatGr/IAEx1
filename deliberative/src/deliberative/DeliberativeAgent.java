package deliberative;

/* import table */
import logist.simulation.Vehicle;

import java.util.ArrayList;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeAgent implements DeliberativeBehavior {

	enum Algo { BFS, ASTAR, NAIVE }
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	
	/* the properties of the agent */
	Agent agent;

	/* the algorithm */
	Algo algo;
	Algorithm algorithm;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.agent = agent;
		
		// initialize the planner
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algo = Algo.valueOf(algorithmName.toUpperCase());
		switch (algo) {
		case ASTAR:
			algorithm = new AStar();
			break;
		case BFS:
			algorithm = new BFS();
			break;			
		//default:
		//	throw new AssertionError("Should not happen.");
		}
		
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {	
		if (algo == Algo.NAIVE) {
			return naivePlan(vehicle, tasks);
		}
		State initState = new State(vehicle.getCurrentCity(), tasks,
				vehicle.getCurrentTasks(), vehicle.capacity());
		return algorithm.plan(initState);
	}
	
	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {} // vehicle.getCurrentTasks() is called in plan so this is useless
}
