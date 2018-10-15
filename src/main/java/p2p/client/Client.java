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

package p2p.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import p2p.chord.BootStrap;
import p2p.chord.Node;
import p2p.data.Sentence;

public class Client {

	public static void main(String args[]) {

		try {
			Node chord = (Node) Naming.lookup("//localhost:8000/Node");
			System.out.println("Le client est connecte a chord\n");
			while (true) {
				BufferedReader stdIn = new BufferedReader(
						new InputStreamReader(System.in));
				System.out.println("\n-----------------------");
				System.out.println("1) Ajouter un objet");
				System.out.println("2) Recuperer un objet");
				System.out.println("0) Quitter le client");
				System.out.println("---> ");
				int chx = Integer.parseInt(stdIn.readLine().trim());
				//System.out.print("key = ");
				//int key = Integer.parseInt(stdIn.readLine().trim());
				switch (chx) {
				case 1:
					System.out.print("message = ");
					Sentence s = new Sentence(stdIn.readLine());
					int key = s.hashCode() % (int) Math.pow(2, BootStrap.SPACESIZE -1);
					chord.addObject(key, s);
					System.out.println("Objet envoye [key="+key+"].");
					break;
				case 2:
					System.out.print("key = ");
					int keytoget = Integer.parseInt(stdIn.readLine().trim());
					Sentence res = (Sentence) chord.getObject(keytoget);
					System.out.println("objet recuper�: " + res);
					break;
				case 0:
					System.out.println("=> Client exit.");
					System.exit(0);
					break;
				default:
					/* nothing to do */
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ConnectException e) {
			System.err.println("Chord unreachable : " + e.getMessage());
			System.err.println("\nSystem exit");
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
