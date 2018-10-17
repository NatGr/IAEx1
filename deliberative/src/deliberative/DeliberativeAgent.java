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
		case NAIVE:
			algorithm = new Naive();
		}
		
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;
		State initState = new State(vehicle.getCurrentCity(), tasks,
				vehicle.getCurrentTasks(), vehicle.capacity());
		
		long deltaTime = System.nanoTime();
		System.out.println("Start Computing a plan");
		plan = algorithm.plan(initState);
		deltaTime = System.nanoTime() - deltaTime;
		System.out.println("Time elapsed (s) for " + tasks.size() + " tasks with " + algo + ": " + (deltaTime/1000000000));
		System.out.println("Plan total cost is of " + plan.totalDistance() + "km");
		System.out.println("\n");
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {} // vehicle.getCurrentTasks() is called in plan so this is useless
}
