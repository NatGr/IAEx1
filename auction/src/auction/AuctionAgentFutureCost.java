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
	private int nbrArtificialTasks = 5;  // artificial tasks are tasks that are likely to occur that we use in the beginning of the
	// auction to compute our marginal costs
	private int artificialTasksUsageLimit = 3;  // we will stop using artificial tasks once we have artificialTasksUsageLimit tasks
	private boolean madeTransitionArtificialTasks = false;
	private ArrayList<Integer> artificalTasksWeights;
	private ArrayList<City> artificalTasksPickupCities;
	private ArrayList<City> artificalTasksDeliverCities;
	private long bidGain = 350;
	private long minBidGain = 0;  // once we stopped using ArtificialTasks, we will make the bid gain addapt to the difference between our
	// bid and the opponent's while staying in these bounds
	private long maxBidGain = 1000;
	private double updateFactorUp = 0.6; // update factor we use when increasing the bidGain
	private double updateFactorDown = 0.9; // bigger than updateFactorUp because loosing a task to the opponent out of greed is much worse than
	// not making that much profit by delivering it

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
			if (tasks.size() <= artificialTasksUsageLimit) {
				artificalTasksWeights.remove(artificalTasksWeights.size()-1);
				artificalTasksPickupCities.remove(artificalTasksPickupCities.size()-1);
				artificalTasksDeliverCities.remove(artificalTasksDeliverCities.size()-1);
			}
			tasks.remove(tasks.size()-1);  // we remove the task from our 
			// task set since we will not deliver it
		}
		
		// we adapt the bid gain
		if (tasks.size() > artificialTasksUsageLimit) {  // we don't do it on the transition to not using art. tasks
			Long oppBid, ourBid;
			if (agent.id() == 0) {
				oppBid = bids[1];
				ourBid = bids[0];
			} else {
				oppBid = bids[0];
				ourBid = bids[1];
			}
			
			if (oppBid == null) {
				oppBid = ourBid + maxBidGain - bidGain; // if he bid null, we assume he bid a lot and we should increase our bid
			}
			
			double updateFactor = (ourBid > oppBid) ? updateFactorDown : updateFactorUp;
			bidGain += (long) (updateFactor*(oppBid - ourBid));
			bidGain = Math.min(Math.max(bidGain, minBidGain), maxBidGain); // we assure ourselves to stay in the boundaries
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
		} else if (!madeTransitionArtificialTasks && tasks.size() == artificialTasksUsageLimit ) { // in that case, we go from using 
			// the artificial tasks to not using them
			long timeOutPrevSol = timeOut - bidTimeLimit/2;
			prevSol = mlc.getSolution(tasks, timeOutPrevSol, true);
			madeTransitionArtificialTasks = true;
		}
				
		tasks.add(task); // we add it no matter what and will remove it if we don't get the task
		if (tasks.size() <= artificialTasksUsageLimit) { // equal since we added one to the task size right before
			artificalTasksWeights.add(task.weight);
			artificalTasksPickupCities.add(task.pickupCity);
			artificalTasksDeliverCities.add(task.deliveryCity);
			newSolWithTask = mlc.getSolution(artificalTasksWeights, artificalTasksPickupCities, artificalTasksDeliverCities, timeOut, true);
		} else {
			newSolWithTask = mlc.getSolution(tasks, timeOut, true);
		}

		double bid = Math.max(1, newSolWithTask.cost - prevSol.cost) + bidGain; // if prevSol.cost > newSolWithTask.cost, the cost is negative because
		// the stochastic search did not find a goos solution and we should not use that to compute the bid
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