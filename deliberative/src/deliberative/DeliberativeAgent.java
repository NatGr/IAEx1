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
		State initState = new State(vehicle.getCurrentCity(), tasks,
				vehicle.getCurrentTasks(), vehicle.capacity());
		return algorithm.plan(initState);
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {} // vehicle.getCurrentTasks() is called in plan so this is useless
}
