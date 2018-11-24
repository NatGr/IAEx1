package auction;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

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
public class AuctionAgentFutureCost implements AuctionBehavior {

	private ArrayList<Task> tasks;
	private Agent agent;
	private MarginalLossComputer mlc;
	private Solution prevSol;
	private Solution newSolWithTask;  // new solution if we get the task we bid for
	private long planTimeLimit;
	private long bidTimeLimit;
	private int maxVehicleCapacity = 0;
	private int nbrArtificialTasks = 5;
	private boolean madeTransitionArtificialTasks = false;
	private ArrayList<Integer> artificalTasksWeights;
	private ArrayList<City> artificalTasksPickupCities;
	private ArrayList<City> artificalTasksDeliverCities;
	private long bidGain = 350;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		long start = System.currentTimeMillis();
		this.tasks = new ArrayList<Task>();
		this.agent = agent;
		this.mlc = new MarginalLossComputer(agent);
		this.prevSol = null;
		this.newSolWithTask = null;
		this.artificalTasksWeights = new ArrayList<Integer>();
		this.artificalTasksPickupCities = new ArrayList<City>();
		this.artificalTasksDeliverCities = new ArrayList<City>();
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
        long setupTimeLimit = ls.get(LogistSettings.TimeoutKey.SETUP);
        
        
        // Compute the nbrArtificialTasks most probable tasks
        PriorityQueue<TaskProba> pq = new PriorityQueue<TaskProba>();
        for (City pickup: topology.cities()) {
        	for (City deliver: topology.cities()) {
        		pq.add(new TaskProba(distribution.probability(pickup, deliver), pickup, deliver, distribution.weight(pickup, deliver)));
        	}
        }
        
        for (int i=0; i<nbrArtificialTasks; i++) {
        	TaskProba tp = pq.poll();
        	artificalTasksWeights.add(tp.weight);
        	artificalTasksPickupCities.add(tp.pickupCity);
        	artificalTasksDeliverCities.add(tp.deliveryCity);
        }
       
        if (setupTimeLimit > bidTimeLimit/2) { // in that case, we compute the cost of the 
        	// solution with only the first tasks here, otherwise we will compute it in the askPrice method
        	prevSol = mlc.getSolution(artificalTasksWeights, artificalTasksPickupCities,
        			artificalTasksDeliverCities, start + setupTimeLimit - 50, true);
        }
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		if (winner == agent.id()) {
			prevSol = newSolWithTask;
		} else {
			if (tasks.size() <= nbrArtificialTasks / 2) {
				artificalTasksWeights.remove(artificalTasksWeights.size()-1);
				artificalTasksPickupCities.remove(artificalTasksPickupCities.size()-1);
				artificalTasksDeliverCities.remove(artificalTasksDeliverCities.size()-1);
			}
			tasks.remove(tasks.size()-1);  // we remove the task from our 
			// task set since we will not deliver it
		}
	}
	
	@Override
	public Long askPrice(Task task) {
		if (task.weight > maxVehicleCapacity) {
			return null;
		}
		long timeOut = System.currentTimeMillis() + bidTimeLimit - 35;
		
		if (prevSol == null) {
			long timeOutPrevSol = timeOut - bidTimeLimit/2;
			prevSol = mlc.getSolution(artificalTasksWeights, artificalTasksPickupCities, artificalTasksDeliverCities, timeOutPrevSol, true);
		} else if (!madeTransitionArtificialTasks && tasks.size() == nbrArtificialTasks / 2 ) { // in that case, we go from using 
			// the artificial tasks to not using them
			long timeOutPrevSol = timeOut - bidTimeLimit/2;
			prevSol = mlc.getSolution(tasks, timeOutPrevSol, true);
			madeTransitionArtificialTasks = true;
		}
				
		tasks.add(task); // we add it no matter what and will remove it if we don't get the task
		if (tasks.size() <= nbrArtificialTasks / 2) { // equal since we added one to the task size right before
			artificalTasksWeights.add(task.weight);
			artificalTasksPickupCities.add(task.pickupCity);
			artificalTasksDeliverCities.add(task.deliveryCity);
			newSolWithTask = mlc.getSolution(artificalTasksWeights, artificalTasksPickupCities, artificalTasksDeliverCities, timeOut, true);
		} else {
			newSolWithTask = mlc.getSolution(tasks, timeOut, true);
		}

		System.out.println(newSolWithTask.cost);
		System.out.println(prevSol.cost);
		double bid = Math.max(1, newSolWithTask.cost - prevSol.cost) + bidGain;
		return (long) Math.round(bid);
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		long timeOut = System.currentTimeMillis() + planTimeLimit - 40;
		
		ArrayList<Task> list = new ArrayList<Task>();
        for (Task task: tasks) {
        	list.add(task);
        }
		Solution newSol = mlc.getSolution(list, timeOut, true);
		return newSol.getPlans(list);
	}
}