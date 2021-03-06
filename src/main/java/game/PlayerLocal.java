/*
 * Gomoku
 * Maciej Kawecki 2015/16
 */
package game;

import gui.BoardGraphics;
import java.io.IOException;
import network.Client;
import network.Command;
import network.MessageReader;

/**
 *
 * Szablon obiektu reprezentującego gracza-człowieka, zdalnego klienta w grze sieciowej
 * 
 * @author Maciej Kawecki
 * @see MouseHandler
 * 
 */
public class PlayerLocal extends Player {
    
	/** Referencja do obiektu klienta */ 
   private final Client client;

   /**
    * Konstruktor 
    * @param client Referencja do obiektu klienta
    * @param pieceColor Kolor kamieni gracza
    * @param gBoard Referencja do obiektu będącego graficzną reprezentacją planszy
    * @param lBoard Referencja do obiektu logicznej warstwy planszy
    * @param name Nazwa gracza
    * @param params Parametry (nazwa)
    */
   public PlayerLocal(Client client, BoardFieldState pieceColor, 
                BoardGraphics gBoard, Board lBoard, String name,  Object... params) {
       
     super(pieceColor, gBoard, lBoard, name, params);
     this.client = client;
       
   }  
   
   
   /**
    * Nadpisana metoda superklasy, wymuszająca zakończenie ruchu gracza
    */
   @Override
   public void forceEndTurn() {
       
     super.forceEndTurn();
     
     if (!client.getPing().isPingOut())
     try {
        client.sendCommand(new Command(Command.CMD_EXIT));        
     } catch (Exception e) {
        client.endGame();
     }
    
   }   
   
   
  
   /**
    * Metoda obsługuje wykonanie ruchu przez gracza, wykorzystując obsługę zdarzeń myszy
    * @see MouseHandlerLocalClient
    */   
   @Override
   public void makeMove() {
    
     // utworzenie wątku oczekującego na wiadomości
     MessageReader msgReader = new MessageReader(client);
     msgReader.start();
     
     // ustawienie obsługi zdarzeń myszy
     final MouseHandlerLocalClient moveHandler
              = new MouseHandlerLocalClient(client, gBoard, lBoard, pieceColor);   
     final MouseHandlerLocalClient cursorHandler
              = new MouseHandlerLocalClient(client, gBoard, lBoard, pieceColor);      
     gBoard.addMouseListener(moveHandler);
     gBoard.addMouseMotionListener(cursorHandler);

     // oczekiwanie na ustawienie kamienia lub wymuszenie zakończenia ruchu
     Integer tmp = lBoard.freeFieldsAmount;
     do {} while (!gameRestarted && tmp.equals(lBoard.freeFieldsAmount));
    
     lastMove = moveHandler.getMove();
     
     //  koniec kolejki, więc trzeba usunąć listenery myszy
     gBoard.removeMouseListener(moveHandler);
     gBoard.removeMouseMotionListener(cursorHandler);
     // przywrócenie domyślnego kursora myszy
     gBoard.setDefaultMouseCursor();
     
     // zwolnienie wątku oczekującego na wiadomości
     try {
       client.sendCommand(new Command(Command.CMD_STOP_MSG));
     } catch (IOException e) {
       System.err.println(e);
     } catch (ClassNotFoundException e) {
       System.err.println(e);
     }
     finally {
       msgReader.interrupt();    
     }
     
        
   }
    

    
}