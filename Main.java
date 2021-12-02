import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.lang.InterruptedException;
import java.net.URL;
import java.util.Scanner;
import javax.imageio.ImageIO;

/**
 * Still-frame webcam scraper that creates a timelapse gif file.
 * Takes input for source, duration, frame count, and save location.
 * Stores all scraped image files in subdirectory in jpeg format.
 * Calls AnimatedGifEncoder to create timelapse gif from stored images.
 * 
 * NOTE: 
 * I am not the author of AnimatedGifEncoder.java - Retrieved online from 
 * http://www.java2s.com/Code/Java/2D-Graphics-GUI/AnimatedGifEncoder.htm
 *
 * @author Andy Anderson
 * @version v1
 */

public class Main {

  /**
   * Collects input from user for image scraping parameters.
   * Ensures all input is valid.
   * Calls methods to scrape the images and create gif.
   */
  public static void main(String[] args) {
    String[][] imageSources = { // {name, url, refreshTime},
      {"UW Bothell Campus", "http://69.91.192.220/netcam.jpg", "60"},
      {"UW Seattle Campus", "https://www.washington.edu/cambots/camera1_l.jpg", "300"},
      {"Kaloch Lodge - Olympic National Park" , "https://pixelcaster.com/dnc-kalaloch/kalaloch.jpg", "60"},
      {"Friday Harbor Ferry", "https://images.wsdot.wa.gov/wsf/fridayharbor/friholding.jpg", "120"},
      {"Sequim Valley Airport", "http://olypen.com/sequimvalleyairport/webcam/webcam.jpg", "45"},
      {"Poulsbo, WA", "http://bwbryant.com/poulsbo_webcam.jpg", "900"},
      {"Olympia Airport", "https://images.wsdot.wa.gov/airports/OlySW.jpg", "900"}
    }; // lots more freeway/port webcams can be found at https://www.king5.com/traffic-cameras
    Scanner scanner = new Scanner(System.in);
    int arrayLength = get2dArrayLength(imageSources);
    System.out.println();
    printHeader("Available webcams");
    for (int i = 0; i < arrayLength; i++) {
      System.out.println((i + 1) + ": " + imageSources[i][0]);
    }
    System.out.print("\nEnter which webcam feed you want a timelapse of: ");
    int imageSourceIndex = (scanner.nextInt() - 1);
    if (imageSourceIndex >= 0 && imageSourceIndex <= (arrayLength - 1)) {
      String imageURL = imageSources[imageSourceIndex][1];
      int refreshTime = Integer.valueOf(imageSources[imageSourceIndex][2]); // in seconds
      System.out.print("Enter how many hours the timelapse should cover: ");
      double duration = scanner.nextDouble();
      if (duration > 0) {
        int maxFrames = (int) (Math.ceil((3600 * duration) / refreshTime));
        System.out.print("Enter how many frames should be used (max " + maxFrames + "): ");
        int frameCount = scanner.nextInt();
        if (frameCount > 0 && frameCount <= maxFrames) {
          double minimumLength = (Math.ceil((1000 * frameCount) / 60.0) / 1000); // round up three decimals
          System.out.print("Enter how many seconds long the final timelapse should be (min " + removeTrailingZeros(minimumLength) + "): ");
          double timelapseLength = scanner.nextDouble();
          int delay = (int) ((1000 * timelapseLength) / frameCount);
          if (delay > (1000 / 60)) { // minimum delay for a gif, ~60fps 
            System.out.print("Enter directory to save images and timelapse: ");
            String savePath = scanner.next();
            System.out.println();
            long waitTime = (long) ((3600000 * duration) / frameCount); // ms between frames, needs long datatype
            BufferedImage[] downloadedImages = imageFetcher(imageURL, savePath, waitTime, frameCount);
            System.out.println("\nImages downloaded, creating timelapse gif...");
            gifCreator(downloadedImages, savePath, delay);
            System.out.println();
          } else {
            System.out.println("Timelapse length is too short");
          }
        } else {
          System.out.println("Frame count must be greater than 0 and at most " + maxFrames);
        }    
      } else {
      System.out.println("Duration must be greater than 0 hours");
      }    
    } else {
      System.out.println((imageSourceIndex + 1) + " is not a valid selection");
    }
  }

  /**
   * Removes all trailing zeros from a double value and returns it as a String.
   */
  public static String removeTrailingZeros(double number) {
    String returnString = ("" + number);
    while (true) {
      char lastChar = returnString.charAt(returnString.length() - 1);
      if (lastChar == '0' || lastChar == '.') {
        returnString = returnString.substring(0, (returnString.length() - 1));
      } else {
        return returnString;
      }
    }
  }

  /**
   * Prints out input parameter and creates line underneath of equal length.
   */
  public static void printHeader(String givenString) {
    System.out.println(givenString);
    for (int i = 0; i < givenString.length(); i++) {
      System.out.print("-");
    }
    System.out.println();
  }

  /**
   * Returns a count of how many String 1D arrays exist within a 2D array.
   */
  public static int get2dArrayLength(String[][] array) {
    int count = 0;
    for (String[] subarray : array) {
      count++;
    }
    return count;
  }

  /**
   * Creates a directory if it doesn't already exist and deletes all files from it.
   */
  public static void cleanDirectory(String path) {
    new File(path).mkdirs();
    File[] files = new File(path).listFiles();
    if (files.length > 0) {
      for (File file : files) {
        file.delete();
      }
    }
  }

  /**
   * Scrapes and saves images from specified URL at specified intervals to specified path.
   * Returns array of all scraped images as BufferedImages.
   */
  public static BufferedImage[] imageFetcher(String imageURL, String savePath, long waitTime, int frameCount) {
    savePath += "\\timelapseSourceImages";
    BufferedImage[] images = new BufferedImage[frameCount];
    int digitsInFrameCount = String.valueOf(frameCount).length();
    cleanDirectory(savePath);
    for (int i = 0; i < frameCount; i++) {
      String formattedImageCount = String.format("%0" + digitsInFrameCount + "d", i + 1); // needed for image sorting
      String truePath = savePath + "\\image" + formattedImageCount + ".jpg";
      try {
        images[i] = ImageIO.read(new URL(imageURL));
        ImageIO.write(images[i], "jpg", new File(truePath));
        System.out.println("(" + formattedImageCount + "/" + frameCount + ") New image saved to " + truePath);
      } catch (IOException e) {
        System.out.println("Error saving to " + truePath);
      }
      if (i != (frameCount - 1)) { // skips wait time on last image
        try {
          Thread.sleep(waitTime);
        } catch (InterruptedException e) {
          System.out.println("Error handling reloading delay");
        }
      }
    }
    return images;
  }

  /**
   * Uses AnimatedGifEncoder class to create a new gif file according to specifications.
   * Takes BufferedImage array and iterates through, adding each as a frame to the gif.
   * Saves gif to specified path once complete.
   */
  public static void gifCreator(BufferedImage[] sourceImages, String savePath, int delay) {
    AnimatedGifEncoder gif = new AnimatedGifEncoder();
    savePath += "\\timelapse.gif";
    try {
      OutputStream file = new FileOutputStream(savePath);
      gif.start(file);
      gif.setDelay(delay);
      for (BufferedImage image : sourceImages) {
        if (image != null) {
          gif.addFrame(image);
        }
      }
      gif.finish();
      System.out.println("Created gif successfully! - Saved to " + savePath);
    } catch (IOException e) {
      System.out.println("Error with gif creation");
    }
  }
}