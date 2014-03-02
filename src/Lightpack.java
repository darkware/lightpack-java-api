import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Locale;

public class Lightpack {

    private final Socket socket;
    private final int[] ledMap;
    private boolean haveLock;

    /**
     * Initializes the connection to the Lightpack
     * @param host Hostname (e.g. 127.0.0.1)
     * @param port Port (default: 3636)
     * @param ledMap Array of the LED Mapping
     * @throws IOException
     */
    public Lightpack(String host, int port, int[] ledMap) throws IOException {
        socket = new Socket(host, port);
        this.haveLock = false;
        // Defensively copy the led map so that we have an immutable object
        this.ledMap = new int[ledMap.length];
        for (int i = 0, max = ledMap.length; i < max; i++) {
            this.ledMap[i] = ledMap[i];
        }
    }

    /**
     * Returns the Saved profiles on the Lightpack
     * @return Array of available Profiles
     * @throws IOException
     */
    public String[] getProfiles() throws IOException {
        String command = "getprofiles\n";
        String response = sendAndReceive(command);
        return response.replaceAll(";\r\n", "").split(":")[1].split(";");
    }

    /**
     * Returns the currently active Profile
     * @return Name of the currently active Profile
     * @throws IOException
     */
    public String getProfile() throws IOException {
        String command = "getprofile\n";
        String response = sendAndReceive(command);
        return response.split(":")[1];
    }

    /**
     * Returns the Lightpack Status
     * @return Status of the Lightpack
     * @throws IOException
     */
    public String getStatus() throws IOException {
        String command = "getstatus\n";
        String response = sendAndReceive(command);
        return response;
    }

    /**
     * Returns the LED Count
     * @return LED Count
     * @throws IOException
     */
    public int getCountLeds() throws IOException {
        String command = "getcountleds\n";
        String response = sendAndReceive(command);
        return Integer.valueOf(response.split(":")[1].trim());
    }

    /**
     * Returns the API Status
     * @return API Status
     * @throws IOException
     */
    public String getApiStatus() throws IOException {
        String command = "getstatusapi\n";
        String response = sendAndReceive(command);
        return response.split(":")[1];
    }


    /**
     * @param ledNumber number of the LED for which the color will be changed
     * @param red value between [0 - 255] inclusive
     * @param green value between [0 - 255] inclusive
     * @param blue value between [0 - 255] inclusive
     */
    public String setColor(int ledNumber, int red, int green, int blue) throws IOException {
        String command = String.format(Locale.US, "setcolor:%d-%d,%d,%d;\n", ledNumber, red, green, blue);
        return sendAndReceive(command);
    }

    /**
     * @param red value between [0 - 255] inclusive
     * @param green value between [0 - 255] inclusive
     * @param blue value between [0 - 255] inclusive
     */
    public String setColorForAll(int red, int green, int blue) throws IOException {
        StringBuilder command = new StringBuilder("setcolor:");
        for (int led : ledMap) {
            command.append(String.format(Locale.US, "%d-%d,%d,%d;", led, red, green, blue));
        }
        command.append('\n');
        return sendAndReceive(command.toString());
    }

    /**
     * @param gamma value between [0.01 - 10.0] inclusive
     */
    public String setGamma(double gamma) throws IOException {
        System.out.println(gamma);
        String command = String.format(Locale.US, "setgamma:%.1f\n", gamma);
        System.out.println(command);
        return sendAndReceive(command);
    }

    /**
     * @param smoothness value between [0 - 255] inclusive
     */
    public String setSmoothness(int smoothness) throws IOException {
        String command = String.format(Locale.US, "setsmooth:%d\n", smoothness);
        return sendAndReceive(command);
    }

    /**
     * @param brightness value between [0 - 100] inclusive
     */
    public String setBrightness(int brightness) throws IOException {
        String command = String.format(Locale.US, "setbrightness:%d\n", brightness);
        return sendAndReceive(command);
    }

    /**
     * Changes Lightpack Profile
     * @param profile
     * @return
     * @throws IOException
     */
    public String setProfile(String profile) throws IOException {
        String command = String.format(Locale.US, "setprofile:%s\n", profile);
        return sendAndReceive(command);
    }

    /**
     * Turns on the Lightpack
     * @return Response from the Lightpack
     * @throws IOException
     */
    public String turnOn() throws IOException {
        return sendAndReceive("setstatus:on\n");
    }

    /**
     * Turns off the Lightpack
     * @return Response from Lightpack
     * @throws IOException
     */
    public String turnOff() throws IOException {
        return sendAndReceive("setstatus:off\n");
    }

    private String sendAndReceive(String command) throws IOException {

        // Convert the command to bytes
        byte[] bytes = null;
        try {
            bytes = command.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // Write the output
        socket.getOutputStream().write(bytes);
        socket.getOutputStream().flush();

        // get the response
        InputStreamReader reader = new InputStreamReader(socket.getInputStream());
        CharBuffer buf = CharBuffer.allocate(8192);
        reader.read(buf.array(),0,8192);
        return buf.toString();
    }

    /**
     * Resolves an API Lock (Needed for color changes etc)
     * @return Whether the API Lock could have been established
     * @throws IOException
     */
    public boolean lock() throws IOException {
        String command = "lock\n";
        String response = sendAndReceive(command);
        boolean isLocked = response.contains("lock:success");
        if(isLocked){
            haveLock = true;
        } else {
            haveLock = false;
        }
        return isLocked;
    }

    /**
     * Releases the API Lock
     * @return Whether the API Lock has been successfully removed (or wasn't locked in the first place)
     * @throws IOException
     */
    public boolean unlock() throws IOException {
        String command = "unlock\n";
        String response = sendAndReceive(command);
        boolean unlocked = response.contains("unlock:success") || response.contains("unlock:not locked");
        if(unlocked){
            haveLock = false;
        } else {
            haveLock = true;
        }
        return unlocked;
    }

    /**
     * No further communication with the Lightpack will be possible after this method is called.
     */
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Lightpack)) {
            return false;
        }

        Lightpack lightpack = (Lightpack) o;

        return Arrays.equals(ledMap, lightpack.ledMap) && socket.equals(lightpack.socket);
    }

    @Override
    public int hashCode() {
        int result = socket.hashCode();
        result = 31 * result + Arrays.hashCode(ledMap);
        return result;
    }

    @Override
    public String toString() {
        return "Lightpack{" +
                "socket=" + socket +
                ", ledMap=" + Arrays.toString(ledMap) +
                '}';
    }
}
