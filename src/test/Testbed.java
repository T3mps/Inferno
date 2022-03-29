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

        for (int i = 0; i < 10; i++) {
            Entity entity = registry.create();
            entity.onComponentAdd.register(componentListener);
            entity.add(new TransformComponent());
            if (Math.random() > 0.5) entity.add(new RigidbodyComponent());
            if (Math.random() > 0.5) entity.add(new RenderComponent(Math.random() * 10));
        }

        for (int i = 0; i < 32; i++) registry.update(0.016f);
        
        registry.dispose();
    }

    static class PhysicsSystem extends IterativeIntervalSystem {

        private static final Family family = Family.define(TransformComponent.class, RigidbodyComponent.class);

        public PhysicsSystem() {
            super(family, 0.016f);
        }

        @Override
        protected void processEntity(Entity entity) {
            
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

            System.out.println("RenderSystem: Position(" + transform.x + ", " + transform.y + "), z-Index(" + render.getZIndex() + ")");
        }

    }

    static class TransformComponent extends AbstractComponent {

        public float x, y;
        public float rotation;

        public TransformComponent() {
            this(0f, 0f, 0f);
        }

        public TransformComponent(float x, float y, float rotation) {
            this.x = x;
            this.y = y;
            this.rotation = rotation;
        }
    
    }
    
    static class RigidbodyComponent extends AbstractComponent {

        public float x, y;
        public float xVelocity, yVelocity;
        public float rotation;

        public RigidbodyComponent() {
            this(0f, 0f, 0f);
        }

        public RigidbodyComponent(float xVelocity, float yVelocity, float rotation) {
            this.xVelocity = xVelocity;
            this.yVelocity = yVelocity;
            this.rotation = rotation;
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