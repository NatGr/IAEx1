package auction;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.List;

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
public class AuctionAgent implements AuctionBehavior {

	private ArrayList<Task> tasks;
	private ArrayList<Task> opponentTasks;
	private Agent agent;
	private MarginalLossComputer mlc;
	private Solution prevSol;
	private Solution newSolWithTask;  // new solution if we get the task we bid for
	private Solution prevOppSol;
	private Solution newOppSolWithTask;
	private long planTimeLimit;
	private long bidTimeLimit;
	private int maxVehicleCapacity = 0;
	private boolean tryToPredictOpponent = true;
	private ArrayList<Integer> oppStartCities;  // the guessed starting cities of the opponent
	private int nbrVehicles;
	private List<City> cities;
	private int avgCostPerKm = 0;
	private double oppComputedBid;
	private int nbrIter = 10;
	private int bidGain1 = 0;
	private int bidGain2 = 750;
	private int ennemyPredictedGain = 500;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.tasks = new ArrayList<Task>();
		this.opponentTasks = new ArrayList<Task>();
		this.agent = agent;
		this.mlc = new MarginalLossComputer(agent);
		this.prevSol = null;
		this.prevOppSol = null;
		this.newSolWithTask = null;
		this.nbrVehicles = agent.vehicles().size();
		this.cities = topology.cities();
		
		for(Vehicle vehicle: agent.vehicles()) {
			avgCostPerKm += vehicle.costPerKm();
		}
		avgCostPerKm /= nbrVehicles;
				
		this.oppStartCities = new ArrayList<Integer>();
		
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
		Long oppBid = bids[(agent.id() == 0) ? 1 : 0];
		if (tryToPredictOpponent) {
			System.out.println("We predicted " + oppComputedBid + ", Opponent played " + oppBid);
		}
		
		boolean addedCityOpponent = false;
		
		if (opponentTasks.size() == 1 && oppStartCities.size() < nbrVehicles) {  // we will try to refine our estimation of the opponent base city
			// if he hasn't carried any tasks yet (opponentTasks.size() == 1) and we haven't found all of its vehicles (assuming he has as much as we do)
			if (oppBid != null) {
				double bestCityScores = Double.MAX_VALUE, currentCityScore;
				int bestCityIndex = 0;
				double taskDistance = previous.pickupCity.distanceTo(previous.deliveryCity);
				
				for (int i=0; i < cities.size(); i++) {
					currentCityScore = Math.abs(oppBid - avgCostPerKm * (cities.get(i).distanceTo(previous.pickupCity) + taskDistance) - ennemyPredictedGain);
					// the closest to zero, the more likely it is the adversary came from that city, assuming the adversary took no benefit
					if (currentCityScore < bestCityScores) {
						bestCityScores = currentCityScore;
						bestCityIndex = i; // assuming min is unique
					}
				}
								
				boolean alreadyIn = false; // done instead of a set since size is smaller than 5
				for (int city: oppStartCities) {
					if (city == bestCityIndex) {
						alreadyIn = true;
						break;
					}
				}
				if (!alreadyIn) {
					oppStartCities.add(bestCityIndex);
					mlc.updateOpponent(oppStartCities, maxVehicleCapacity, avgCostPerKm, cities);
				}
			}
		}
		
		if (tryToPredictOpponent && oppComputedBid != 0 && !addedCityOpponent) { // if we tried to compute a bid and if our bid wasn't 
			// wrong because it came from a city we hadn't previously encountered
			if (Math.abs(oppComputedBid - oppBid) > 500) { // in that case, our city estimation is probably wrong and we should drop it
				tryToPredictOpponent = false;
				System.out.println("We won't try to predict his moves anymore");
			} else {
				double upDateFactor = 0.7;
				ennemyPredictedGain = ennemyPredictedGain + (int) (upDateFactor*(oppBid - oppComputedBid)); // exponential averaging
			}
		}

		if (winner == agent.id()) {
			prevSol = newSolWithTask;
			opponentTasks.remove(opponentTasks.size()-1); // we remove the task from the 
			// task set since it will not deliver it
		} else {
			prevOppSol = newOppSolWithTask;
			tasks.remove(tasks.size()-1);
		}
	}
	
	@Override
	public Long askPrice(Task task) {
		if (task.weight > maxVehicleCapacity) {
			return null;
		}
		double prevCost = (prevSol == null) ? 0 : prevSol.cost, bid = 0;
		long currentTime = System.currentTimeMillis();
		
		tasks.add(task);  // we add it no matter what and will remove it if we don't get the task
		opponentTasks.add(task);
		
		if (tryToPredictOpponent && oppStartCities.size() > 0) { // we try to predict the opponents moves
			double prevOppCost = (prevOppSol == null) ? 0 : prevOppSol.cost;
			double fracTime =  ((double) tasks.size()) / (opponentTasks.size() + tasks.size()) * bidTimeLimit;
			long timeOut1 = currentTime + (long) fracTime;
			long timeOut2 = currentTime + bidTimeLimit - 50;
			
			newSolWithTask = mlc.getSolution(tasks, timeOut1, true);
			double frac = ((double) tasks.size()) / nbrIter;
			double bidGain = (frac > 1) ?  bidGain2 : bidGain1*(1.-frac) + bidGain2*frac;
			double dummyBid = Math.max(1, newSolWithTask.cost - prevCost) + bidGain;
			
			newOppSolWithTask = mlc.getSolution(opponentTasks, timeOut2, false);
			oppComputedBid = Math.max(1, newOppSolWithTask.cost - prevOppCost + ennemyPredictedGain);
			
			if ((oppComputedBid < dummyBid) && (oppComputedBid > dummyBid - 1000)) {
				bid = oppComputedBid + 150;
			} else {
				bid = dummyBid;
			}
		} else {
			long timeOut = currentTime + bidTimeLimit - 35;
			newSolWithTask = mlc.getSolution(tasks, timeOut, true);

			double frac = ((double) tasks.size()) / nbrIter;
			double bidGain = (frac > 1) ?  bidGain2 : bidGain1*(1.-frac) + bidGain2*frac;
			bid = Math.max(1, newSolWithTask.cost - prevCost) + bidGain;
			
		}
		return (long) Math.round(bid);
	}
	

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		System.out.println("the ennemy predicted cities were");
		for (int i = 0; i < oppStartCities.size(); i++) {
			System.out.println(cities.get(oppStartCities.get(i)));
		}
		
		long timeOut = System.currentTimeMillis() + planTimeLimit - 40;
		
		ArrayList<Task> list = new ArrayList<Task>();
        for (Task task: tasks) {
        	list.add(task);
        }
		Solution newSol = mlc.getSolution(list, timeOut, true);
		return newSol.getPlans(list);
	}
}
