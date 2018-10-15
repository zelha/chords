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

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NodeImpl extends UnicastRemoteObject implements Node {

	private static final long serialVersionUID = -1194581004263559244L;

	/** Node Identifier */
	private String ip;

	// /** Node port */
	// private int port = 0;

	/** The node IP after hashing */
	private int id;

	/** id MAX */									// m : m-bit identifier
	private static int MAXid = (int) Math.pow(2, BootStrap.SPACESIZE - 1) - 1;
				// the -1 here is because in java a table starts at index 0 .. (obviously)
	/** id MIN */
	private static int MINid = 0;

	/** Node predecessor */
	private NodeImpl predecessor;
	/** Node successor */
	private NodeImpl successor;

	/** Fingers table */                        //todo : check -1 ou moins 1 non ?
	private NodeImpl[] fingersTable = new NodeImpl[BootStrap.SPACESIZE];
	                                    //because k's maximum is SPACESIZE m

	/** hash Table */
	private Map<Integer, Object> table;

	/** index of the next finger */
	private int next = 0;  //todo

	/** Boolean for the Stay Stable thread */
	private boolean alive = true;

	/** Time between each stabilization/fix fingerstable */
	private int timeToCheck = 500; //todo à modifier si ça bug //5secondes

	public NodeImpl() throws RemoteException {
	}

	public NodeImpl(String ip) throws RemoteException {
		this(ip, null, null);
		successor = this;
		predecessor = this;

		//this(ip,this,this);
        // fill fingertables from
		for (int i = 1; i < BootStrap.SPACESIZE; i++) //todo corriger : (int i = 1; en int i = 0 ? ..
			fingersTable[i] = this;
		checkStable();
	}

	public NodeImpl(String ip, NodeImpl pred, NodeImpl succ)
			throws RemoteException {
		this.ip = ip;
		predecessor = pred;
		successor = succ;
		id = hash();
		table = new HashMap<Integer, Object>();

		if (succ != null)
			findSuccessor(this.id); //3lach
	}

	// ///////////////////// //
	// TOOLS //
	// ///////////////////// //
	private static boolean ifNodeIn(int id, int begin, int end) {
		return (begin < end && begin <= id && id <= end)
				|| (begin > end && ((begin <= id && id <= MAXid) || (MINid <= id && id <= end)))
				|| ((begin == end) && (id == begin));
	}


    //todo again, 3lach
	public void addObject(int key, Object o) throws RemoteException {
		//in range
	    if (predecessor == null || ifNodeIn(key, this.getPredecessor().id, id)) {
			table.put(key, o);
		} else { //if not then add to the next
			this.findSuccessor(key).addObject(key, o);
		}
	}

	public Object getObject(int key) throws RemoteException {
		if (predecessor == null || ifNodeIn(key, this.getPredecessor().id, id)) {
			return table.get(key);
		} else {
			return this.findSuccessor(key).getPredecessor().getObject(key);
		}
	}

	// ////////////////////// //
	// ALGO //
	// ////////////////////// //
	/**
	 * ask node n (this) to find the successor of id
	 */
	public synchronized NodeImpl findSuccessor(int id) {
		if (ifNodeIn(id, this.id /* aka n */ + 1, this.getSuccessor().getId())) {
			return this.getSuccessor();
		} else {
			NodeImpl x = this.closestPrecedingNode(id);
			return x.findSuccessor(id);
		}
	}



	public synchronized NodeImpl closestPrecedingNode(int id) {
		for (int i = BootStrap.SPACESIZE - 1; i > 0; i--) {
			if (this.fingersTable[i].isAlive()
					&& ifNodeIn(this.fingersTable[i].getId(), this.id + 1, id - 1)) //pourquoi -1 cette fois  ????
				return this.fingersTable[i];
		}
		return this;
	}

	/**
	 * join a Chord ring containing node n
	 */
	public synchronized void join(NodeImpl n) {
		this.predecessor= n.closestPrecedingNode(this.id); //different than FIG 6
		this.successor = n.findSuccessor(this.id);
	}

	/**
	 * called periodically. verifies n's immediate successor, and tells the
	 * successor about n.
	 */
	public synchronized void stabilize() {
		NodeImpl x = successor.getPredecessor();
		if (ifNodeIn(x.getId(), this.id + 1, this.getSuccessor().getId() - 1)) {
			this.setSuccessor(x);// = x;
		}
		this.getSuccessor().notify(this);
	}

	/**
	 * n thinks it might be our predecessor.
	 */
	public synchronized void notify(NodeImpl n) {
		if ((predecessor == this) //todo add || predecessor == null ?
				|| (ifNodeIn(n.getId(), predecessor.getId() + 1, this.id - 1))) {
			this.setPredecessor(n);

			//todo 3lach
			ArrayList<Integer> keysToRemove = new ArrayList<Integer>();
			for (int key : table.keySet()) {
				if (n.id > key) {
					try {
						n.addObject(key, table.get(key));
					} catch (RemoteException e) {
						e.printStackTrace();
					} //add them to list of key to remove
					keysToRemove.add(key);
				}
			}
			for (int key : keysToRemove) {
				table.remove(key);
			}
		}
	}

	/**
	 * called periodically. refreshes finger table entry n.
	 */
	private synchronized void fixFingersTable() {
		this.next++;
		if (next > BootStrap.SPACESIZE- 1)
			this.next = 1;
		this.fingersTable[this.next] = this.findSuccessor((id + (int) Math.pow(2,
				next - 1))
				% (int) Math.pow(2, BootStrap.SPACESIZE - 1));
	}

	/**
	 * checks whether predecessor has failed.
	 */
	public void checkPredecessor() {
        System.out.println("[WARNING] Check predecessor :  no implementation ");
		// if(fail(predecessor)){
		// predecessor = null;
		// }
	}

	public int hash() {
		return Integer.valueOf(ip);
	}

	/**
	 * Kill the node (clean)
	 */
	public synchronized void kill() {
		alive = false;
		if(predecessor==null)predecessor=this;
		if(successor==null)successor=this;

		for (int key : table.keySet()) {
			try {
				this.addObject(key, table.get(key)); //intuition rlly
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

 

	// ////////////////////// //
	// Thread - Stabilization //
	// ////////////////////// //
	/**
	 * Run a thread for the stabilization of the node
	 */
	private void checkStable() {
		new Thread(new Runnable() {
			public void run() {
				while (alive) {
					//System.err.println("\tStabilization in progress for Node "
					 //+ getId());
					stabilize();
					fixFingersTable();
					try {
						Thread.sleep(timeToCheck);
					} catch (InterruptedException e) {System.out.println("checkStable Error");
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	// ///////////////// //
	// Display //
	// ///////////////// //
	public synchronized String toString() {
		String res = "<NODE: " + id + ", PRED: "
				+ ((this.getPredecessor() == this ||  this.getPredecessor() == null) ? null : this.getPredecessor().getId())
				+ ", SUCC: "
				+ (this.getSuccessor() == this ? null : this.getSuccessor().getId()) + "> ";
		res += "\n\tFingers Table: ";
		if (fingersTable[1] != null) {
			res += "[";
			for (int i = 1; i < BootStrap.SPACESIZE - 1; i++) {
				res += "" + this.getFingersTable()[i].getId() + ", ";
			} //todo intellij tells me to use string builder ...
			res += "" +  this.getFingersTable()[BootStrap.SPACESIZE - 1].getId() + "]";
		} else {
			res += "null";
		}
		// affichage du contenu de la table.
		if (!table.isEmpty()) {
			res += "\n\tData Content : ";
			for (Map.Entry<Integer, Object> entry : table.entrySet()) {
				res += "\n\t  [" + entry.getKey() + "] - ";
				res += entry.getValue().toString();
			}
		}
		res += "\n\n";
		return res;
	}

	// ///////////////// //
	// Getter and Setter //
	// ///////////////// //
	public String getIp() {
		return ip;
	}

	public int getId() {
		return id;
	}

	public synchronized NodeImpl getPredecessor() {
		return predecessor;
	}

	public synchronized void setPredecessor(NodeImpl predecessor) {
		this.predecessor = predecessor;
	}

	public synchronized NodeImpl getSuccessor() {
		return successor;
	}

	public synchronized void setSuccessor(NodeImpl successor) {
		this.successor = successor;
	}

	public NodeImpl[] getFingersTable() {
		return fingersTable;
	}

	public void setFingersTable(NodeImpl[] fingersTable) {
		this.fingersTable = fingersTable;
	}

	public boolean isAlive() {
		return alive;
	}
}
