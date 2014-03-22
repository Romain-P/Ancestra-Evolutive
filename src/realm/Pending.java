package realm;

import objects.Compte;
import tool.time.waiter.Waiter;

import common.SocketManager;

public class Pending {

    /*
     TODO : Gestion du paquet Af + position dans la file d'attente.
    */
	private static Waiter waiter = new Waiter(); //très laid mais ce putin de Pending est à refaire !
    public static void PendingSystem(final Compte C)
    {
        if(C == null) return;
        if(C._position <= 1)
        {
        	waiter.addNext(new Runnable() {
        		public void run() {
        			if(C == null || C.getRealmThread() == null) return;
                	SocketManager.MULTI_SEND_Af_PACKET(C.getRealmThread(),1,RealmServer._totalAbo,RealmServer._totalNonAbo,""+1,RealmServer._queueID);
                	C._position = -1;
                	RealmServer._totalAbo--;
        		}
        	}, 750);
        } else {
        	waiter.addNext(new Runnable() {
        		public void run() {
        			if(C == null ||  C.getRealmThread() == null) return;
            		SocketManager.MULTI_SEND_Af_PACKET(C.getRealmThread(),1,RealmServer._totalAbo,RealmServer._totalNonAbo,""+1,RealmServer._queueID);
            		C._position = -1;
            		RealmServer._totalAbo--;
        		}
        	}, 750*C._position);
        }
    }
}