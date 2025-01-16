package lab4;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Stack;

import lab4.znakovi.Konstanta;
import lab4.znakovi.PrijevodnaJedinica;

public class GeneratorKoda {

    public static void main(String[] args) throws IOException {

        // ---- parsiraj input u stablo ---- \\
        // not tested

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line = reader.readLine();
        int spaceCount = 0;
        int newSpaceCount = 0;
        Stack<Node> s = new Stack<Node>();
        Node parent = null;

        Node root = Node.createNode(line.strip()); // always PrijevodnaJedinica
        s.push(root);

        while (null != (line = reader.readLine()) && line.length() != 0) {
            // System.out.println(line);
            newSpaceCount = line.length() - line.replaceAll("^\\s+", "").length();

            if (spaceCount > newSpaceCount) {
                for (int i = 0; i < spaceCount - newSpaceCount; i++) {
                    s.pop();
                }
            }

            spaceCount = newSpaceCount;

            if (line.strip().startsWith("<")) {
                Node newnode = Node.createNode(line.strip()); // child node

                parent = s.pop();
                parent.children.add(newnode);
                // System.out.println("parent --" + parent.toString() + " child -- " +
                //         newnode.toString());
                s.push(parent);
                s.push(newnode); // current level

            } else {
                // spaceCount = spaceCount + 1;

                parent = s.pop();
                String[] linel = line.strip().split(" ");

                Konstanta gk = new Konstanta(linel[0], Integer.parseInt(linel[1]), linel[2]);
                gk.parent = parent; // hopefully this is fine
                parent.children.add(gk);

                s.push(parent);
            }
        }
        // postfiksObidji(root); // for testing

        // ---- obidji stablo ---- \\

        Generator analizator = new Generator();

        analizator.analiziraj((PrijevodnaJedinica) root);

    }

    // for testing
    private static void obidji(Node node) {
        System.out.println(node.toString());
        for (Node n : node.children) {
            obidji(n);
        }
        System.out.println("--- no more children");
    }

    // for testing
    private static void postfiksObidji(Node node) {
        System.out.println(node.toString());
        System.out.println("djeca:");
        for (Node n : node.children) {
            System.out.println(n.toString());
        }
        System.out.println("--- no more children");
        for (Node n : node.children) {
            postfiksObidji(n);
        }
    }

}
