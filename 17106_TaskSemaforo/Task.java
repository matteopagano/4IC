package task;

import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Florian
 */
public class Task implements Runnable {

    public static final String RESET = "\u001B[0m";
    public static final String ROSSO = "\u001B[31m";
    public static final String VERDE = "\u001B[32m";

    CodaTask coda;// coda a cui deve chiedere la risorsa
    Risorsa[] risorse;
    private int[] risorsaCheMiInteressa;

    public Task(CodaTask coda, Risorsa[] risorsa, int[] posRisorsa) {
        this.coda = coda;
        this.risorse = risorsa;
        this.risorsaCheMiInteressa = posRisorsa;
    }

    @Override
    public void run() {
        System.out.printf(VERDE + " " + Thread.currentThread().getName() + " Pronto ad usare la risorsa :D\n" + RESET);
        try {
            // Richiedo alla coda se posso accedere alla risorsa, se la risorsa é gia occupata da qualcunaltro(thread)
            //allora la coda mi mettera in coda e appena sarà libero di nuovo potrò accedere alla risorsa.
            for (int i = 0; i < risorsaCheMiInteressa.length; i++) {
                coda.accediRisorsa(getRisorsa(risorsaCheMiInteressa[i]), Thread.currentThread());
            }
        } catch (InterruptedException ex) {
        }
        System.out.printf(Thread.currentThread().getName() + ": Ho finito di usare la risorsa :D\n");
    }

    private Risorsa getRisorsa(int i) {
        return this.risorse[i]/*.getRisorsaPreziosa()*/;
    }
}

final class CodaTask {

    private final int MASSIMO_NUMERI_DI_RISORSE; // il numero di risorse :D
    private final Random r;
    private final Semaphore semaforo;  // il nostro semaforo

    public CodaTask(final int nPermessi) {
        r = new Random();   // Non metto la data come seme, mi fido.
        this.MASSIMO_NUMERI_DI_RISORSE = nPermessi;
        this.semaforo = new Semaphore(MASSIMO_NUMERI_DI_RISORSE);
    }

    public void accediRisorsa(Risorsa risorsa, Thread daQualeThread) throws InterruptedException {
        /*
        //Spiegato nel file "commento.dm" la differenza tra drainPermits e availablePermits...secondo me :D
        if (semaforo.drainPermits() == MASSIMO_NUMERI_DI_RISORSE - 1) {  // Non ce nemmeno 1 permesso disponibile
            // Sono curioso di sapere che stato ha il thread se invece le/a risorse/a sono occupate :D
            Thread osserva = new Thread(new Osserva(daQualeThread));// Osservo come si comporterà il mio thread
            osserva.start(); // Osservo...
        }
         */
        // mi prendo la risorsa , se il numero di permessi del semaforo sono 0 mi mette in coda d'attesa:
        synchronized (risorsa) {
            /*
            SYNCHRO-MOTHEFUCKING-NIZED é ciò che mi permette di sincronizzare il semaforo tenendo conto di questa risorsa
            Quindi se un thread ha gia provato ad accedere a questa risorsa prendendosi il acquirement del semaforo io non entrerò qui fino a quando
            la risorsa non sarà libera di nuovo!
            */
            semaforo.acquire(1);// in realta scrivere acquire() é come scrivere acquire(1)
            //Una volta acquisita la risorsa la maneggio per random secondi :
            utilizzaRisorsa(risorsa);
            // ora la risorsa tornerà ad essere libera quindi riutilizzabile
            semaforo.release(1);// in realta scrivere release() é come scrivere release(1)
        }
    }

    private double tempoCasuale(int minimo, int massimo) {
        // nextDouble restituisce un double casuale da 0 a 1
        return minimo + r.nextDouble() * (minimo + massimo);
    }

    private void utilizzaRisorsa(Risorsa risorsa) throws InterruptedException {
        int massimo = 4, minimo = 1;
        double durataUtilizzo = tempoCasuale(minimo, massimo);// da 1 a 4 secondi
        long durataNanoSecondi = (long) (durataUtilizzo * 1000000.0);// convertire da Secondi (con la virgola) a nanosecondi
        int nCicli = 3 + r.nextInt(3);
        System.out.println(Thread.currentThread().getName() + " sta usato la risorsa : " + Task.ROSSO + risorsa.getRisorsaPreziosa() + Task.RESET + " ...");
        int percentualeAttesa = 0;
        String ben = durataUtilizzo < (massimo + minimo) / 2 ? "solo" : "ben";
        String coloreScritta = ben.equals("ben") ? Task.ROSSO : Task.VERDE; // Se ha impiegato molto tempo il task lo scrivo in rosso altrimenti in verde
        Thread.sleep(20);//Un po di distacco dal eventuale messaggio precedente
        // Il metodo utilizza oltre a fare tutta questa(sabaniflorian) scena modifica anche la risorsa:
        String alfabeto = "abcdefghflorianilmnopqrsabanituvzABCDEFGHILMNOPQRSTUVZ";
        for (int i = 0; i < nCicli; i++) {
            System.out.println("... " + percentualeAttesa + "%"+ "\t\t\t Risorsa : ["+""+risorsa+""+Task.RESET+"];" + "\t valore : "+risorsa.getRisorsaPreziosa());  // prettamente figo.
            TimeUnit.MICROSECONDS.sleep(durataNanoSecondi / nCicli);
            percentualeAttesa += 100 / nCicli;
            risorsa.setValoreRisorsaPreziosissima(risorsa.getRisorsaPreziosa()+alfabeto.charAt(r.nextInt(alfabeto.length())));  // modifico la risorsa :D
        }
        System.out.println("Finito ... " + 100 + "%");  // scrivo 100 per evitare il problema del 99%.

        System.out.println(Thread.currentThread().getName() + " ha usanto la risorsa : " + risorsa.getRisorsaPreziosa() + " per " + coloreScritta + ben + " " + durataUtilizzo + Task.RESET + " secondi");
    }

    class Osserva implements Runnable {

        Thread daOsservare;

        public Osserva(Thread daOsservare) {
            this.daOsservare = daOsservare;
        }

        @Override
        public void run() {
            System.out.println(aggiorna());
        }

        private String aggiorna() {
            return "Il Thread : " + daOsservare.getName() + "  IN CODA CON LO STATO : " + daOsservare.getState().name();
        }
    }
}

class Risorsa {

    private String valoreRisorsaPreziosissima;
    protected boolean isOccupata;

    public Risorsa(String risorsaPreziosa) {
        this.valoreRisorsaPreziosissima = risorsaPreziosa;
    }

    public String getRisorsaPreziosa() {
        this.setIsOccupata(true);
        return valoreRisorsaPreziosissima;
    }

    public void rilasciaRisorsa() {
        this.setIsOccupata(false);
    }

    public void setValoreRisorsaPreziosissima(String valoreRisorsaPreziosissima) {
        this.valoreRisorsaPreziosissima = valoreRisorsaPreziosissima;
    }

    public void setIsOccupata(boolean isOccupata) {
        this.isOccupata = isOccupata;
    }

    public boolean getIsOccupata() {
        return isOccupata;
    }
                                               }
