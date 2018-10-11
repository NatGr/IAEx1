package deliberative;

import java.util.ArrayList;

import logist.task.Task;
import logist.topology.Topology;
import logist.topology.Topology.City;
import uchicago.src.collection.Pair;

public class State {
	Topology td;
	City c;
	ArrayList<Task> tasks;
	ArrayList<City> cities;
	
	public void createChildren() {
		ArrayList<State> children = new ArrayList<State>();
		/*for (Pair p: packages) {
			for (City c: (City)p.second) {
				children.add(this.movePackage(packages, (Task)p.first, c));
			}
		}*/
		
				
	}
	
	/*public ArrayList<Pair> movePackage(State s, Task t, City c) {
		
		ArrayList<Pair> pck = (ArrayList<Pair>) packages.clone();
		/*for (Pair p: pck) {
			if(p.first.equals(t)) {
				p.second = c;
				return pck;
			}
		}
		return pck;
	}
	*/

}
