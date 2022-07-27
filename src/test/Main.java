package test;

import java.util.UUID;

import com.temprovich.inferno.Component;
import com.temprovich.inferno.Entity;
import com.temprovich.inferno.Inferno;
import com.temprovich.inferno.Registry;

public class Main {

    private Registry registry;

    // simulation parameters

    private int initialEntityCount = 10;
    private int iterations = 100;

    // end simulation parameters

    private Main() {
        this.registry = new Registry();

        for (int i = 0; i < initialEntityCount; i++) {
            Entity entity = Inferno.create();
            String name = "Entity " + (i + 1);
            String uuid = UUID.randomUUID().toString();
            entity.add(new TagComponent(name, uuid));

            float rng = (float) Math.random();

            if (rng < 0.5) {
                entity.add(new TransformComponent(0, 0, 0));
            } else {
                entity.add(new RenderComponent(Math.random() * 255, Math.random() * 255, Math.random() * 255));
            }

            registry.add(entity);
        }
    }

    private void loop() {
        for (int i = 0; i < iterations; i++) {
            registry.update(0.016f);
        }
    }
    
    public static void main(String[] args) {
        new Main();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Test Components ////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    private static final class TransformComponent extends Component {
            
            public int x;
            public int y;
            public float rotation;

            public TransformComponent(int x, int y, float rotation) {
                this.x = x;
                this.y = y;
                this.rotation = rotation;
            }
    }

    private static final class RenderComponent extends Component {
            
            public double r, g, b;
    
            public RenderComponent(double r, double g, double b) {
                this.r = r;
                this.g = g;
                this.b = b;
            }
    }

    private static final class TagComponent extends Component {
            
            public String tag;
            public String uuid;
            
            public TagComponent(String tag, String uuid) {
                this.tag = tag;
                this.uuid = uuid;
            }
    }
}
