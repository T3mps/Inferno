# Inferno
Fast, lightweight entity component system (ECS) implemented in Java.


## Getting Started
Below is a simple example demostrating the features of Inferno.

```Java
class Inferno {

	static class Position {
		
		float x;
		float y;
		
		public Position(float x, float y) {
			this.x = x;
			this.y = y;
		}
		
		@Override
		public String toString() {
			return "x: " + x + ", y: " + y;
		}
	}
	
	record Velocity(float x, float y) {
	}
	
	record Rigidbody() {
	}
	
	public static void main(String[] args) {
		Registry registry = new Registry();
		
		EventSink sink = registry.eventSink();
		sink.connect(ListenerType.ON_COMPONENT_ADD, EventSink.ANY, (entity, component) -> {
			System.out.println(component.getClass().getSimpleName() + " added to entity " + entity.getFormattedID());
		});
		sink.connect(ListenerType.ON_COMPONENT_ADD, Rigidbody.class, (entity, component) -> {
			System.out.println(entity.getFormattedID() + " added to PhysicsWorld");
		});
		
		Entity entity = registry.emplace(new Position(0, 0), new Velocity(1, 1), new Rigidbody());

		Scheduler scheduler = registry.createScheduler();
		scheduler.submit(() -> {
			registry.view(Position.class, Velocity.class).stream().forEach(view -> {
				var position = view.component1();
				var velocity = view.component2();
				position.x += velocity.x;
				position.y += velocity.y;
				
				System.out.println(view.entity().toString());
			});
		});
		for (int i = 0; i < 100; ++i) {
			scheduler.update();
		}
		
		String json = registry.serialize(entity);
		registry.close();
		
		Registry registry2 = new Registry();
		registry2.deserialize(json);
		registry2.view(Position.class).stream().forEach(System.out::println);
		registry2.close();
	}
}
```
