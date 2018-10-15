package deliberative;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import logist.plan.Action;
import logist.plan.Plan;

public class AStar implements Algorithm {
	
	public static Comparator<State> StateComparator = new Comparator<State>() {
		public int compare(State s1, State s2) {
			System.out.println("compare");
			return s1.compareTo(s2);
		}
	};

	@Override
	public Plan plan(State initState) {
		System.out.println("Calculate plan");
		ArrayList<State> queue = new ArrayList<State>();
		ArrayList<State> finalStates = new ArrayList<State>();
		queue.add(initState);
		
		State state = initState;
		// BFS
		while(!queue.isEmpty()) {
			if (state.isTerminal()) {
				finalStates.add(state);
			} else {
				for (State s: state.createChildren()) {
					queue.add(s);
				}
			}
			queue.sort(StateComparator);
			state = queue.remove(0);
		}
		
		// finding best plan, i.e. the one leading to a final state with the smallest distance:
		Plan bestPlan = null, currentPlan = null;
		double bestDistance = Double.MAX_VALUE, currentDistance;
		for (State finalState: finalStates) {
			currentPlan = finalState.getPlan();
			currentDistance = currentPlan.totalDistance();
			if (currentDistance < bestDistance) {
				bestPlan = currentPlan;
			}
		}
		return bestPlan;
	}
	
	
	
	

}
