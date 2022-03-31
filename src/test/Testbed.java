package test;

import com.temprovich.apollo.Entity;
import com.temprovich.apollo.EntityListener;
import com.temprovich.apollo.Family;
import com.temprovich.apollo.Registry;
import com.temprovich.apollo.component.AbstractComponent;
import com.temprovich.apollo.signal.SignalListener;
import com.temprovich.apollo.system.IterativeIntervalSystem;
import com.temprovich.apollo.system.IterativeSystem;

class Testbed {

    static final Registry registry = new Registry();

    public static void main(String[] args) {
        EntityListener physicsListener = new EntityListener() {
            @Override
            public void onEntityAdd(Entity entity) {
                System.out.println("Entity added to physics world");
            }

            @Override
            public void onEntityRemove(Entity entity) {
                System.out.println("Entity removed from physics world");
            }
        };

        EntityListener renderListener = new EntityListener() {
            @Override
            public void onEntityAdd(Entity entity) {
                System.out.println("Entity added to render list");
            }

            @Override
            public void onEntityRemove(Entity entity) {
                System.out.println("Entity removed from render list");
            }
        };

        SignalListener<Entity> componentListener = new SignalListener<Entity>() {
            @Override
            public void receive(Entity entity) {
                System.out.println("Component added to entity");
            }
        };

        Family physicsFamily = Family.define(TransformComponent.class, RigidbodyComponent.class);
        Family renderFamily = Family.define(TransformComponent.class, RenderComponent.class);

        registry.bind(new PhysicsSystem());
        registry.bind(new RenderSystem());

        registry.register(physicsListener, physicsFamily);
        registry.register(renderListener, renderFamily);

        for (int i = 0; i < 16; i++) {
            Entity entity = Registry.create();
            entity.onComponentAdd.register(componentListener);
            entity.add(new TransformComponent());
            if (Math.random() > 0.5) entity.add(new RigidbodyComponent());
            if (Math.random() > 0.5) entity.add(new RenderComponent(Math.random() * 100));
            registry.add(entity);
        }

        for (int i = 0; i < 16; i++) registry.emplace(new TransformComponent(), 
                                                      new RigidbodyComponent(),
                                                      new RenderComponent(Math.random() * 100))
                                                      .onComponentAdd
                                                      .register(componentListener);

        for (int i = 0; i < 32; i++) registry.update(0.016f);
        
        registry.dispose();
    }

    static class PhysicsSystem extends IterativeIntervalSystem {

        TransformComponent transform;
        RigidbodyComponent rigidbody;

        public PhysicsSystem() {
            super(Family.define(TransformComponent.class, RigidbodyComponent.class), 0.016f);
        }

        @Override
        protected void processEntity(Entity entity) {
            transform = entity.get(TransformComponent.class);
            rigidbody = entity.get(RigidbodyComponent.class);

            rigidbody.xAcceleration = Math.random() / rigidbody.mass;
            rigidbody.yAcceleration = Math.random() / rigidbody.mass;

            rigidbody.xVelocity += rigidbody.xAcceleration * (0.5 * interval);
            rigidbody.yVelocity += rigidbody.yAcceleration * (0.5 * interval);

            transform.x = rigidbody.x += rigidbody.xVelocity * interval;
            transform.y = rigidbody.y += rigidbody.yVelocity * interval;

            System.out.println("Physics update: <" + transform.x + ", " + transform.y + ">");
        }

    }

    static class RenderSystem extends IterativeSystem {

        public RenderSystem() {
            super(Family.define(TransformComponent.class, RenderComponent.class));
        }

        @Override
        protected void process(Entity entity, float dt) {
            TransformComponent transform = entity.get(TransformComponent.class);
            RenderComponent render = entity.get(RenderComponent.class);

            System.out.println("Render update: <" + transform.x + ", " + transform.y + ">, z: " + render.zIndex);
        }

    }

    static class TransformComponent extends AbstractComponent {

        public double x, y;
        public double rotation;

        public TransformComponent() {
            this(0, 0, 0);
        }

        public TransformComponent(double x, double y, double rotation) {
            this.x = x;
            this.y = y;
            this.rotation = rotation;
        }
    
    }
    
    static class RigidbodyComponent extends AbstractComponent {

        public double x, y;
        public double xVelocity, yVelocity;
        public double xAcceleration, yAcceleration;
        public double rotation;
        public double mass;

        public RigidbodyComponent() {
            this(0, 1);
        }

        public RigidbodyComponent(double rotation, double mass) {
            this.x = 0;
            this.y = 0;
            this.xVelocity = 0;
            this.yVelocity = 0;
            this.xAcceleration = 0;
            this.yAcceleration = 0;
            this.rotation = rotation;
            this.mass = mass;
        }

    }

    static class RenderComponent extends AbstractComponent {

        private double zIndex;
    
        public RenderComponent(double zIndex) {
            this.zIndex = zIndex;
        }
    
        public double getZIndex() {
            return zIndex;
        }
        
    }

}