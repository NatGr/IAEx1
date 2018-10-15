package deliberative;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import logist.plan.Plan;

public class BFS implements Algorithm {

	@Override
	public Plan plan(State initState) {
		System.out.println("Create plan");
		Queue<State> queue = new LinkedList<State>();
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
			state = queue.poll();
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
