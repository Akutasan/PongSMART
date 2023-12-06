import java.awt.*;
import java.awt.image.BufferedImage;

public class ML {
    public static double distance = 0;

    public static void main(String[] args) {
        Pong activeGame = new Pong();
    }

    public static boolean bestAction(double position) {

        // Actions: Go up, Go down
        if (distance - position > 0) {
            return true;
        }
        else {
            return false;
        }
    }

    public BufferedImage preprocessImage(BufferedImage image) {
        BufferedImage grayImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = grayImage.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        Image tmp = grayImage.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
        BufferedImage downscaledImage = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
        g = downscaledImage.getGraphics();
        g.drawImage(tmp, 0, 0, null);
        g.dispose();

        return downscaledImage;
    }

    public MultiLayerNetwork createNetwork() {
        int numInputs = 6400;  // 80*80 input pixels
        int numOutputs = 1;  // Up or down action
        int numHiddenNodes = 200;

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .updater(new Adam())
                .weightInit(WeightInit.XAVIER)
                .list()
                .layer(new DenseLayer.Builder().nIn(numInputs).nOut(numHiddenNodes)
                        .activation(Activation.RELU)
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .activation(Activation.SOFTMAX)
                        .nIn(numHiddenNodes).nOut(numOutputs).build())
                .build();

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        return model;
    }


}
