package deliberative;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import logist.plan.Plan;

public class AStar implements Algorithm {
	
	@Override
	public Plan plan(State initState) {
		/*
		 * DataStructure to iterate over states in order of increasing costs*/		
		PriorityQueue<State> priorityQueue = new PriorityQueue<State>();
		
		/*
		 * Datastructure to see if we have handled a duplicate state before with lower cost.
		 * If so, it doesn't make sense to add it to the priorityqueue.
		 * */
		Map<State, Double> seenStates = new HashMap<State, Double>();
		
		
		
		State state = initState;
		while(state != null) {
			if (state.isTerminal()) {
				//Because we always underestimate the cost, we can return the first state that is terminal since they are stored in a priorityQueue
				return state.getPlan();
			} else {
				for (State s: state.createChildren(true)) {
					Double prevScore = seenStates.get(s);  // will get a state that is equal in term of city, 
					// availableTasks and pickedUpTasks but not necessarily cost
					if (prevScore == null) {
						seenStates.put(s, s.cost + s.heuristic);
						priorityQueue.add(s);
					} else if (s.cost + s.heuristic < prevScore) {
						seenStates.put(s, s.cost + s.heuristic);
						/* we do not remove the previous state from the queue, because we found out that this
						 * slows the algorithm down (O(n) operation), since we add the new state as well, 
						 * the new state's childrens will be put in the queue before the old states'children and
						 * thus his childrens will not be put in the queue so it's indeed better than to explicitely remove it */
						priorityQueue.add(s);
					} // else do nothing
				}
				state = priorityQueue.poll();
			}
		}
		return null; // should not happen
	}
}
