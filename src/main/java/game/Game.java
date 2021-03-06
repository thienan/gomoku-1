/*
 * Gomoku
 * Maciej Kawecki 2015/16
 */
package game;

import java.awt.Color;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.SwingUtilities;

import gomoku.AppObserver;
import gomoku.Lang;
import gomoku.Settings;
import gui.BoardGraphics;
import gui.Console;
import gui.GUI;
import gui.Sounds;
import gui.dialogs.ConfirmDialog;
import gui.dialogs.DialogType;
import gui.dialogs.NewGameDialog;
import network.Client;
import network.Command;


/**
 *
 * Wątek kontrolujący przebieg rozgrywki
 * 
 * @author Maciej Kawecki
 * 
 */
public class Game extends Thread implements Observer {

   /** Pierwszy gracz */
   private Player player1;
   /** Drugi gracz */
   private Player player2;
   /** Logiczna warstwa planszy */
   private Board lBoard;
   /** Graficzna reprezentacja planszy */
   private BoardGraphics gBoard;
   /** Konsola do wyświetlania komunikatów */
   private final Console console;
   /** Ref. do obiektu służącego do odtwarzania dźwięków */
   private final Sounds sounds;
   /** Aktualny stan gry (trwa, oczekiwanie, restart) */
   private GameState gameState;   
   /** Podany przez użytkownika adres IP serwera do przekazania do obiektu klienta */
   private String serverIP;
   /** Obserwacja stanu gry przez wątek serwera */
   private final AppObserver gameSpy;
   /** Obiekt klienta w grze sieciowej */
   private Client client;
   /** Tryb gry */
   private GameMode gameMode;
   /** Ustawienia */
   private Settings settings;
   /** Ref. do GUI */
   private final GUI frame;
   
   
   /**
    * Konstruktor 
    * @param gBoard Graficzna reprezentacja planszy
    * @param console Konsola do wyświetlania komunikatów
    * @param sounds Referencja do obiektu służącego do odtwarzania dźwięków
    * @param gameSpy Referencja do obiektu służącego do komunikacji z tym wątkiem 
    */
   public Game(BoardGraphics gBoard, Console console, Sounds sounds, AppObserver gameSpy) {
     	
     this.gBoard = gBoard;
     this.console = console;
     this.sounds = sounds;
     this.gameSpy = gameSpy; 
     
     frame = ((GUI)(SwingUtilities.getWindowAncestor(console)));
         
   }
   
   
   /**
   * Metoda ustawia referencje przekazane przez obserwatora
   * @param o Obserwowany obiekt 
   * @param object Przekazany obiekt
   */
   @Override
   public void update(Observable o, Object object) {
       
     AppObserver obs = (AppObserver)object;
     
     switch (obs.getKey()) {
   
        // zmieniono stan gry
        case "state":  
       
        	this.gameState = (GameState)obs.getObject();
                   
            // wymuszenie końca ruchu graczy
            if (player1!=null) player1.forceEndTurn();
            if (player2!=null) player2.forceEndTurn();
            // zmiana adresu IP serwera
            this.serverIP = gameState.getServerIP();
            console.networkButtonsEnable(false);    
                
            break;
            
        // zmieniono ustawienia, zmiana ref. do graficznej planszy  
        case "board":
            
           setBoard((BoardGraphics)obs.getObject());
           
           break;
           
           
        // przesłanie wiadomości
        case "message":
            
           String msg = (String)obs.getObject();
           
           try {
             
             client.sendCommand(new Command(Command.CMD_MESSAGE, msg));
             console.newLine();
             console.setMessageLn("[" + Lang.get("SentMsg", msg) + "]", Color.GRAY);
            
           } catch (IOException | ClassNotFoundException e) {
              System.err.println(e);
           }
        
           break;
           
     }
     
   
     
   }
   

   public GameState getGameState() {
       
      return gameState;  
       
   }

   
   public void setBoard(BoardGraphics board) {
       
      gBoard = board; 
       
   }
   
   
   /**
    * Rozpoczęcie nowej rozgrywki.
    * @param gameMode Tryb nowej rozgrywki 
    * @param settings Aktualne ustawienia gry
    * @throws Exception Nie przypisano graczy / nie rozpoczęto rozgrywki
    */
   private void startNewGame(GameMode gameMode, Settings settings) throws Exception {
         
     gameState = GameState.RUN;     
     lBoard = new Board(settings);
     frame.getStatusBar().setVisible(false);

     // przypisanie odpowiednich implementacji gracza w zależności od trybu rozgrywki	 
     switch (gameMode) {     

        // komputer vs gracz
        case SINGLE_GAME:
             
           if (settings.isComputerStarts()) {
             player1 = new PlayerComputer(BoardFieldState.BLACK, gBoard, lBoard, "Computer");
             player2 = new PlayerHuman(BoardFieldState.WHITE, gBoard, lBoard, "PlayerYou", "");
           }
           else {        	  
             player1 = new PlayerHuman(BoardFieldState.BLACK, gBoard, lBoard, "PlayerYou", "");
             player2 = new PlayerComputer(BoardFieldState.WHITE, gBoard, lBoard, "Computer");
           }
           
           frame.getStatusBar().setVisible(true);
            
           break;         
             
        // gracz vs gracz
        case HOTSEAT_GAME:
             
           player1 = new PlayerHuman(BoardFieldState.BLACK, gBoard, lBoard, "Player", 1);
           player2 = new PlayerHuman(BoardFieldState.WHITE, gBoard, lBoard, "Player", 2);
           
           break;  
             
        
        //  gracz (klient lokalny) vs gracz (klient zdalny)
        case NETWORK_GAME:
                    
           try {
                
              client = new Client(serverIP, gameSpy, console);  
              // jeżeli się udało połączyć, to zmiana ustawień gry
              Settings clientSettings = client.getSettings();
              frame.restartClientGameSettings(clientSettings.getColsAndRows());
              settings.setGameSettings(clientSettings.getColsAndRows(), clientSettings.getPiecesInRow());
              gameSpy.sendObject("settings-main", clientSettings);
              
              // zmiana logiki planszy, bo zmiana ustawień
              lBoard = new Board(clientSettings);
              
              // kto pierwszy ten zaczyna
              if (client.getNumber()==0) {
                player1 = new PlayerLocal(client, BoardFieldState.BLACK, gBoard, lBoard,
                		"PlayerYou",  1);
                player2 = new PlayerRemote(client, BoardFieldState.WHITE, gBoard, lBoard,
                		"Player", 2);
              }
              else {
                player1 = new PlayerRemote(client, BoardFieldState.BLACK, gBoard, lBoard,
                		"Player", 1);    
                player2 = new PlayerLocal(client, BoardFieldState.WHITE, gBoard, lBoard,
                		"PlayerYou", 2);   
              }
              
              
              console.networkButtonsEnable(true);
              
           } catch (Exception e) {
               
              try {
                 Thread.sleep(100);  
              } 
              catch (InterruptedException ex) {}
               
              console.setMessage(Lang.get("CantConnect"), Color.RED); 
              
              if (e instanceof ClassNotFoundException) System.err.println(e);
              if (!(e instanceof IOException))
            	  console.setMessage(" " + Lang.get("CantConnectFull"), Color.RED); 
              
              console.newLine();
              
              console.networkButtonsEnable(false);
              player1 = null;
              player2 = null;
                
           }
           
            
           break;
                     
     }
     
     
     // zatrzymanie, jeżeli brakuje graczy lub nie ma połączenia przy grze sieciowej
     if (player1==null || player2==null)  throw new Exception();
 

     sounds.play(Sounds.SND_INFO); 
       
     try {
       Thread.sleep(100);  
     }
     catch(InterruptedException e) {}
     
     console.setMessageLn(Lang.get("START"), new Color(0x22, 0x8b, 0x22));  
     console.newLine();      

     boolean playAgain = false;
     int moveNo = 1;            // nr ruchu
     List<BoardField> winRow;   // lista kamieni w ewentualnym wygrywającym rzędzie
     
     List<Player> players = Arrays.asList(player1, player2); 
     
     // petla rozgrywki
     while (gameState==GameState.RUN) {  
                    
       // sekwencja zdarzeń dla każdego z graczy  
       for(Player p:players) if (gameState==GameState.RUN) {         
    	   
         // komunikat na konsoli
         console.setMessage(Lang.get("Move") + " #" + Integer.toString(moveNo) + ": ", Color.BLUE);
         console.setMessage(p.getName(), (p.getPieceColor()==BoardFieldState.WHITE) ? Color.BLACK : Color.WHITE,
                           (p.getPieceColor()==BoardFieldState.WHITE) ? Color.WHITE : Color.BLACK);
         
         // wykonanie ruchu przez gracza
         p.makeMove();         
         
         // żeby uniknąć wypisywanie komunikatów jeżeli przerwano w trakcie ruchu
         if (gameState!=GameState.RUN) break;
         
         // dokończenie komunikatu na konsoli - wykonany ruch 
         console.setMessageLn("  \u279C  " + lBoard.getFieldName(p.getLastMove()), Color.RED);
         // dźwięk położenia kamienia
         sounds.play(Sounds.SND_MOVE);
         
         // sprawdzenie warunków końca gry (wygrana lub remis)
         winRow = lBoard.getWinningRow(p.getLastMove());
         if ((winRow != null || lBoard.freeFieldsAmount==0)) {
             
           // komunikat o wygranej 
           if (winRow != null) {
               
             console.newLine();  
             console.setMessageLn(Lang.get("WON", p.getName().toUpperCase()), Color.RED);
             console.newGameMsg();
             
             gBoard.setPiecesRow(winRow, p.getPieceColor());
             
             boolean win = (gameMode == GameMode.SINGLE_GAME && p instanceof PlayerHuman) 
            		 || gameMode == GameMode.HOTSEAT_GAME 
            		 || (gameMode == GameMode.NETWORK_GAME && p instanceof PlayerLocal);
             
             sounds.play(Sounds.SND_SUCCESS);
             
             playAgain = new ConfirmDialog(frame, Lang.get("GameOver", moveNo) + "\n\n"
             		+ Lang.get("Won", p.getName()),
             		win ? DialogType.WIN : DialogType.LOOSE).isConfirmed();
             
           }
           
           // komunikat o remisie
           else {
               
             console.newLine();  
             console.setMessageLn(Lang.get("DRAW"), Color.RED);  
             console.newGameMsg();
             
             sounds.play(Sounds.SND_SUCCESS);
             
             playAgain = new ConfirmDialog(frame, Lang.get("GameOver", moveNo) + "\n\n"
             		+ Lang.get("Draw"), DialogType.DRAW).isConfirmed();
                          
             
           }

           gBoard.setDefaultMouseCursor();
           gameState=GameState.WAIT;                      
              
         }
                  
         // odłączenie od serwera
         if (gameState!=GameState.RUN && gameMode==GameMode.NETWORK_GAME && client!=null) {
             
           try {
             client.sendCommand(new Command(Command.CMD_EXIT));
           } catch (Exception e) {}
         
           client.endGame();
           
         }
                    
         moveNo++;         
         
       }
       
       
       try {
    	 Thread.sleep(50);
       }
       catch (InterruptedException e) { gameState=GameState.RESTART; }
                
     } 
     
     if (gameMode == GameMode.NETWORK_GAME && client != null) gBoard.setDefaultMouseCursor();             
     
     if (playAgain) new NewGameDialog(frame);
  
   }
   
   

   @Override
   public void run() {	   
	 
	  try {
		  
	     startNewGame(gameMode, settings);
	     
	  } catch (Exception e) {	    	 	    	
		  
	     gameState=GameState.WAIT;
	     console.newGameMsg();

	  }
	  

	  // pętla oczekiwania na rozpoczęcie nowej gry
	  do {  
	          
	    try {         
	       Thread.sleep(10);
	    } catch (InterruptedException e) { return; }
	     
	  } while (gameState!=GameState.RESTART);
	   
   }
   
   

   public void setGameMode(GameMode gameMode) {
	 this.gameMode = gameMode;
   }
   
   
   public void setSettings(Settings settings) {
	 this.settings = settings;
   }

   
}


