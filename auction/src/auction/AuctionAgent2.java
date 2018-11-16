package auction;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import logist.LogistSettings;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * An auction agent
 * 
 */
public class AuctionAgent2 implements AuctionBehavior {

	private TaskDistribution distribution;
	private ArrayList<Task> tasks;
	private Agent agent;
	private MarginalLossComputer mlc;
	private Solution prevSol;
	private Solution newSolWithTask;  // new solution if we get the task we bid for
	private long planTimeLimit;
	private long bidTimeLimit;
	private int maxVehicleCapacity = 0; //Capacity of the largest vehicle of the agent
	private List<Long[]> bidHistory;  // List of array containing the history of the bids
	private List<Integer> winners;
	private List<Task>[] task_distr;
	// our bids are put at the first position

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.distribution = distribution;
		this.tasks = new ArrayList<Task>();
		this.agent = agent;
		this.mlc = new MarginalLossComputer(agent);
		this.prevSol = null;
		this.newSolWithTask = null;
		this.bidHistory = new ArrayList<Long[]>();
		this.winners = new ArrayList<Integer>();
		for(Vehicle vehicle: agent.vehicles()) {
			if (vehicle.capacity() > maxVehicleCapacity) {
				maxVehicleCapacity = vehicle.capacity();
			}
		}
		
		LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
        // the plan method cannot last more than planTimeLimit milliseconds
        this.planTimeLimit = ls.get(LogistSettings.TimeoutKey.PLAN);
        // the bid method cannot execute more than bidTimeLimit milliseconds
        this.bidTimeLimit = ls.get(LogistSettings.TimeoutKey.BID);
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		// we put our bid in the first position
		Long[] bidsCopy = bids.clone();
		bidsCopy[agent.id()] = bids[0];
		bidsCopy[0] = bids[agent.id()];
		bidHistory.add(bidsCopy);
		winners.add(winner);
		
		if (winner == agent.id()) {
			this.prevSol = this.newSolWithTask;
		} else {
			this.tasks.remove(tasks.size()-1);  // we remove the task from our 
			// task set since we will not deliver it
		}
	}
	
	@Override
	public Long askPrice(Task task) {
		
		if (task.weight > maxVehicleCapacity) {
			return null;
		}
		double prevCost = (prevSol == null) ? 0 : prevSol.cost;
		
		this.tasks.add(task);  // we add it no matter what and will remove it if we don't get the task
		long timeOut = System.currentTimeMillis() + (long) (0.999*bidTimeLimit) - 5;
		// 0.999 for safety + 5ms for the rest of the method
		this.newSolWithTask = mlc.getSolution(this.tasks, timeOut);
		
		// TODO: add time adaptability
		// TODO: add adaptation to ennemy
		// TODO; si on a un cout < 0, on peut suremnt le mettre à 0 comme c'est task specific

		double bid = newSolWithTask.cost - prevCost;
		return (long) Math.round(bid);
	}
	

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		long timeOut = System.currentTimeMillis() + (long) (0.999*planTimeLimit) - 25;
		// 0.999 for safety + 25ms for the plan computing
		
		ArrayList<Task> list = new ArrayList<Task>();
        for (Task task: tasks) {
        	list.add(task);
        }
		Solution newSol = mlc.getSolution(list, timeOut);
		return newSol.getPlans(list);
	}
	
	/**
	 * For every Task in tasks we look at the shortest path from pickup to delivery city.
	 * We then do the same for t2
	 * We return the lenght of the links that are covered by t2 but not by the list of existing tasks
	 * @param tasks: List of current tasks of an agent
	 * @param t2: One possible new task with pickup and delivery city
	 * @return Sum of the distances as decribed above
	 * 
	 */
	public int lengthNewLinks(List<Task> tasks, Task t2) {
		Map<City, Set<City>> covered = new HashMap<City, Set<City>>();
		for (Task t : tasks) {
			List<City> path_t = t.path();
			for (int i=0;i<path_t.size()-1;i++) {
				Set s = covered.get(path_t.get(i));
				if (s == null) {
				    s = new HashSet<City>();
				    covered.put(path_t.get(i), s);
				}
				s.add(path_t.get(i+1));
			}
		}
		int dist = 0;
		List<City> path_t2 = t2.path();
		for (int i = 0; i<t2.path().size()-1; i++) {
			Set<City> s = covered.get(path_t2.get(i));
			if(s != null && s.contains(path_t2.get(i+1))) {
				dist += path_t2.get(i).distanceTo(path_t2.get(i+1));
			}
		}
		return dist;
	}
}
