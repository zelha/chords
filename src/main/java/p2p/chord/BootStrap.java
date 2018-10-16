/*
 * Copyright (C) Inria LogNet team and all the students attending the P2P cours at Unice
 * during years 2013-2017 (precise list available on demand)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package p2p.chord;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 
 *  jChord
 */

/** Simulation Boostrap */
public class BootStrap {

	/**
	 * @apiNote m
	 * and K allant de 1 à M
	 * k allant de 0
	 */
	public static int SPACESIZE = 8; // this is m

	private static List<NodeImpl> nodes;
	private static int sleepTime = 500;//todo à modifier si ça bug
	private static NodeImpl bootstrapNode1;
	private static int nbNode = 1;

	// ////////////////// //
	//       Tools        //
	// ////////////////// //
	public static boolean ifNodeExist(int id){
		for(NodeImpl n : nodes){
			if(n.getId() == id) return true;
		}
		return false;
	}

	// ////////////////// //
	//   Display Chord    //
	// ////////////////// //
	public static void sortNodes(){
		List<NodeImpl> res = new ArrayList<NodeImpl>();
		NodeImpl NodeMin;
		int i,j;
		while(nodes.size() != 0){
			i = 0;
			j = 0;
			NodeMin = nodes.get(0);
			for(NodeImpl n : nodes){
				if(n.getId() < NodeMin.getId()){
					NodeMin = n;
					i = j;
				}
				j++;
			}
			res.add(NodeMin);
			nodes.remove(i);
		}
		nodes = res;
	}

	public static void display() {
		String res = "";
		res += "________________________________________________________\n\n";
		sortNodes();
		for (NodeImpl n : nodes) {
			res += n;
		}
		res += "________________________________________________________\n";
		System.out.println(res);
	}

	// /////////////// //
	//      MAIN       //
	// /////////////// //
	public static void main(String[] args) {
		nodes = new ArrayList<NodeImpl>();
		final Random rand = new Random();

		int id = rand.nextInt((int)Math.pow(2, SPACESIZE)) % (int)Math.pow(2, SPACESIZE-1);
		try {
			bootstrapNode1 = new NodeImpl(id + "");
		} catch (RemoteException e1) {
			e1.printStackTrace();
		}
		nodes.add(bootstrapNode1);
		
		try {
		    @SuppressWarnings("unused")
			Registry registry = LocateRegistry.createRegistry(8000);
		    Naming.rebind("//localhost:8000/Node", bootstrapNode1);
		} catch (Exception e) {
		    e.printStackTrace();
		}

		new Thread(new Runnable(){
			@Override
			public void run() {
				while(true){
					try {
						BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
						System.out.println("#########################");
						System.out.println("# 1) Inserer un node \t#");
						System.out.println("# 2) Retirer un node \t#");
						System.out.println("# 3) Afficher la chord \t#");
						System.out.println("# 0) Quitter jChord \t#");
						System.out.println("#########################");
						System.out.print("--> ");
						int chx = Integer.parseInt(input.readLine().trim());
						int id;
						switch (chx) {
						case 1:
							while(true){
								System.out.print("Node id = " );
								id = Integer.parseInt(input.readLine().trim());
								id = id % (int) Math.pow(2, BootStrap.SPACESIZE -1);
								if(ifNodeExist(id)){
									System.err.println("Node already existing!");
								} else {
									break;
								}
							}
							NodeImpl n = new NodeImpl(id + "");
							nodes.add(n);
							System.out.println("\n--> Adding NODE " + id + "\n");
							// todo need to change this.
                            //URGENT
                                //trying to use a node in the middle to not always use the same one, or no fun
							NodeImpl anode = nodes.get(nodes.size()/2) ;

							n.join(anode);
							nbNode++;
							//these two lines should be in a methode addnode

							break;
						case 2:
							while(true){
								System.out.println("id = " );
								id = Integer.parseInt(input.readLine().trim());
								if(!ifNodeExist(id)){
									System.err.println("Unknow Node " + id);
								} else {
									break;
								}
							}
							for(NodeImpl node : nodes){
								if(node.getId() == id){
									System.err.println("--> Removing NODE " + node.getId() + "\n");
									nodes.remove(node); //.remove(id);
									node.kill();
									nbNode--;
									break;
								}
							}
							break;
						case 3:
							display();
							break;
						case 0:
							System.out.println("=> Chord exit");
							System.exit(0);
							break;
						default:
							break;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
}
