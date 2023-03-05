
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.awt.image.BufferedImage;

public class App {
    private static boolean sphere = false;
    private static ArrayList<Triangle> tris = new ArrayList<>();

    private static HashMap<String, ArrayList<Triangle>> shapes = new HashMap<String, ArrayList<Triangle>>();
    private static String current_shape = "TRIANGLE";

    public static void main(String[] args) {
        SetupShapes();
        tris = shapes.get(current_shape);

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        JFrame controls = new JFrame();
        controls.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container control_pane = controls.getContentPane();
        control_pane.setLayout(null);

        JSlider headingSlider = new JSlider(-180, 180, 0);
        pane.add(headingSlider, BorderLayout.SOUTH);

        JSlider pitchSlider = new JSlider(SwingConstants.VERTICAL, -90, 90, 0);
        pane.add(pitchSlider, BorderLayout.EAST);

        JButton sphereBtn = new JButton("sphere");
        sphereBtn.setBounds(10, 10, 75, 20);
        control_pane.add(sphereBtn);
        JButton shapeBtn = new JButton("shape");
        shapeBtn.setBounds(10, 40, 75, 20);
        control_pane.add(shapeBtn);

        JPanel render_panel = new JPanel() {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());

                double heading = Math.toRadians(headingSlider.getValue());
                Matrix3 heading_transform = new Matrix3(new double[] {
                        Math.cos(heading), 0, -Math.sin(heading),
                        0, 1, 0,
                        Math.sin(heading), 0, Math.cos(heading)
                });
                double pitch = Math.toRadians(pitchSlider.getValue());
                Matrix3 pitch_transform = new Matrix3(new double[] {
                        1, 0, 0,
                        0, Math.cos(pitch), Math.sin(pitch),
                        0, -Math.sin(pitch), Math.cos(pitch)
                });
                Matrix3 transform = heading_transform.multiply(pitch_transform);

                BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

                double[] z_buffer = new double[img.getWidth() * img.getHeight()];
                for (int q = 0; q < z_buffer.length; q++)
                    z_buffer[q] = Double.NEGATIVE_INFINITY;

                for (Triangle t : tris) {
                    Vertex v1 = transform.transform(t.v1);
                    v1.x += getWidth() / 2;
                    v1.y += getHeight() / 2;
                    Vertex v2 = transform.transform(t.v2);
                    v2.x += getWidth() / 2;
                    v2.y += getHeight() / 2;
                    Vertex v3 = transform.transform(t.v3);
                    v3.x += getWidth() / 2;
                    v3.y += getHeight() / 2;

                    Vertex ab = new Vertex(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z);
                    Vertex ac = new Vertex(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z);
                    Vertex norm = new Vertex(
                            ab.y * ac.z - ab.z * ac.y,
                            ab.z * ac.x - ab.x * ac.z,
                            ab.x * ac.y - ab.y * ac.x);

                    double normal_length = Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z * norm.z);

                    norm.x /= normal_length;
                    norm.y /= normal_length;
                    norm.z /= normal_length;

                    double angle_cos = Math.abs(norm.z);

                    int minX = (int) Math.max(0, Math.ceil(Math.min(v1.x, Math.min(v2.x, v3.x))));
                    int maxX = (int) Math.min(img.getWidth() - 1, Math.floor(Math.max(v1.x, Math.max(v2.x, v3.x))));
                    int minY = (int) Math.max(0, Math.ceil(Math.min(v1.y, Math.min(v2.y, v3.y))));
                    int maxY = (int) Math.min(img.getHeight() - 1, Math.floor(Math.max(v1.y, Math.max(v2.y, v3.y))));

                    double triangleArea = (v1.y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - v1.x);

                    for (int y = minY; y <= maxY; y++) {
                        for (int x = minX; x <= maxX; x++) {
                            double b1 = ((y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - x)) / triangleArea;
                            double b2 = ((y - v1.y) * (v3.x - v1.x) + (v3.y - v1.y) * (v1.x - x)) / triangleArea;
                            double b3 = ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / triangleArea;
                            if (b1 >= 0 && b1 <= 1 && b2 >= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1) {
                                double depth = b1 * v1.z + b2 * v2.z + b3 * v3.z;
                                int z_index = y * img.getWidth() + x;
                                if (z_buffer[z_index] < depth) {
                                    img.setRGB(x, y, getShade(t.color, angle_cos).getRGB());
                                    z_buffer[z_index] = depth;
                                }
                            }
                        }
                    }

                }

                g2.drawImage(img, 0, 0, null);
            }
        };
        pane.add(render_panel, BorderLayout.CENTER);

        sphereBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sphere = !sphere;

                if (sphere)
                    for (int i = 0; i < 4; i++)
                        tris = inflate(tris);
                else
                    tris = shapes.get(current_shape);
                render_panel.repaint();
            }
        });
        shapeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (current_shape == "TRIANGLE")
                    current_shape = "SQUARE";
                else
                    current_shape = "TRIANGLE";

                tris = shapes.get(current_shape);

                if (sphere)
                    for (int i = 0; i < 4; i++)
                        tris = inflate(tris);

                render_panel.repaint();
            }
        });

        headingSlider.addChangeListener(e -> render_panel.repaint());
        pitchSlider.addChangeListener(e -> render_panel.repaint());

        frame.setSize(400, 400);
        frame.setVisible(true);

        controls.setSize(200, 200);
        controls.setVisible(true);
    }

    public static Color getShade(Color color, double shade) {
        double red_linear = Math.pow(color.getRed(), 2.4) * shade;
        double green_linear = Math.pow(color.getGreen(), 2.4) * shade;
        double blue_linear = Math.pow(color.getBlue(), 2.4) * shade;

        int red = (int) Math.pow(red_linear, 1 / 2.4);
        int green = (int) Math.pow(green_linear, 1 / 2.4);
        int blue = (int) Math.pow(blue_linear, 1 / 2.4);

        return new Color(red, green, blue);
    }

    public static ArrayList<Triangle> inflate(ArrayList<Triangle> tris) {
        ArrayList<Triangle> result = new ArrayList<>();
        for (Triangle t : tris) {
            Vertex m1 = new Vertex((t.v1.x + t.v2.x) / 2, (t.v1.y + t.v2.y) / 2, (t.v1.z + t.v2.z) / 2);
            Vertex m2 = new Vertex((t.v2.x + t.v3.x) / 2, (t.v2.y + t.v3.y) / 2, (t.v2.z + t.v3.z) / 2);
            Vertex m3 = new Vertex((t.v1.x + t.v3.x) / 2, (t.v1.y + t.v3.y) / 2, (t.v1.z + t.v3.z) / 2);

            result.add(new Triangle(t.v1, m1, m3, t.color));
            result.add(new Triangle(t.v2, m1, m2, t.color));
            result.add(new Triangle(t.v3, m2, m3, t.color));
            result.add(new Triangle(m1, m2, m3, t.color));
        }
        for (Triangle t : result) {
            for (Vertex v : new Vertex[] { t.v1, t.v2, t.v3 }) {
                double l = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z) / Math.sqrt(30000);
                v.x /= l;
                v.y /= l;
                v.z /= l;
            }
        }
        return result;
    }

    private static void SetupShapes() {
        // ? TRIANGLE
        ArrayList<Triangle> triangle_shape = new ArrayList<Triangle>();
        triangle_shape.add(new Triangle(
                new Vertex(100, 100, 100),
                new Vertex(-100, -100, 100),
                new Vertex(-100, 100, -100),
                Color.WHITE));
        triangle_shape.add(new Triangle(
                new Vertex(100, 100, 100),
                new Vertex(-100, -100, 100),
                new Vertex(100, -100, -100),
                Color.RED));
        triangle_shape.add(new Triangle(
                new Vertex(-100, 100, -100),
                new Vertex(100, -100, -100),
                new Vertex(100, 100, 100),
                Color.GREEN));
        triangle_shape.add(new Triangle(
                new Vertex(-100, 100, -100),
                new Vertex(100, -100, -100),
                new Vertex(-100, -100, 100),
                Color.BLUE));

        // ? CUBE
        ArrayList<Triangle> square_shape = new ArrayList<Triangle>();
        // * A
        square_shape.add(new Triangle(new Vertex(-100, 100, 100),
                new Vertex(100, 100, 100),
                new Vertex(-100, 100, -100),
                Color.RED));
        // * B
        square_shape.add(new Triangle(new Vertex(100, 100, 100),
                new Vertex(100, 100, -100),
                new Vertex(-100, 100, -100),
                Color.RED));
        // * C
        square_shape.add(new Triangle(new Vertex(100, -100, 100),
                new Vertex(100, 100, -100),
                new Vertex(100, 100, 100),
                Color.BLUE));
        // * D
        square_shape.add(new Triangle(new Vertex(100, -100, 100),
                new Vertex(100, -100, -100),
                new Vertex(100, 100, -100),
                Color.BLUE));
        // * E
        square_shape.add(new Triangle(new Vertex(-100, -100, 100),
                new Vertex(100, -100, 100),
                new Vertex(-100, 100, 100),
                Color.GREEN));
        // * F
        square_shape.add(new Triangle(new Vertex(100, -100, 100),
                new Vertex(100, 100, 100),
                new Vertex(-100, 100, 100),
                Color.GREEN));
        // * G
        square_shape.add(new Triangle(new Vertex(-100, -100, 100),
                new Vertex(-100, 100, 100),
                new Vertex(-100, -100, -100),
                Color.MAGENTA));
        // * H
        square_shape.add(new Triangle(new Vertex(-100, 100, 100),
                new Vertex(-100, 100, -100),
                new Vertex(-100, -100, -100),
                Color.MAGENTA));
        // * I
        square_shape.add(new Triangle(new Vertex(-100, 100, -100),
                new Vertex(100, 100, -100),
                new Vertex(-100, -100, -100),
                Color.YELLOW));
        // * J
        square_shape.add(new Triangle(
                new Vertex(-100, -100, -100),
                new Vertex(100, 100, -100),
                new Vertex(100, -100, -100),
                Color.YELLOW));
        // * K
        square_shape.add(new Triangle(new Vertex(100, -100, 100),
                new Vertex(-100, -100, 100),
                new Vertex(-100, -100, -100),
                Color.WHITE));
        // * L
        square_shape.add(new Triangle(new Vertex(-100, -100, -100),
                new Vertex(100, -100, -100),
                new Vertex(100, -100, 100),
                Color.WHITE));

        shapes.put("TRIANGLE", triangle_shape);
        shapes.put("SQUARE", square_shape);
    }
}

class Vertex {
    double x;
    double y;
    double z;

    Vertex(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}

class Triangle {
    Vertex v1;
    Vertex v2;
    Vertex v3;
    Color color;

    Triangle(Vertex v1, Vertex v2, Vertex v3, Color color) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.color = color;
    }
}

class Matrix3 {
    double[] values;

    Matrix3(double[] values) {
        this.values = values;
    }

    Matrix3 multiply(Matrix3 other) {
        double[] result = new double[9];
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                for (int i = 0; i < 3; i++) {
                    result[row * 3 + col] += this.values[row * 3 + i] * other.values[i * 3 + col];
                }
            }
        }
        return new Matrix3(result);
    }

    Vertex transform(Vertex in) {
        return new Vertex(
                in.x * values[0] + in.y * values[3] + in.z * values[6],
                in.x * values[1] + in.y * values[4] + in.z * values[7],
                in.x * values[2] + in.y * values[5] + in.z * values[8]);
    }
}