import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import javax.swing.JOptionPane;

enum Size { SMALL, MEDIUM, LARGE }

abstract class Pizza {
    String name;
    Size size;
    boolean cheese;
    abstract int getPrice();
}

class StandardPizza extends Pizza {
    int basePrice;
    
    StandardPizza(String name, Size size, boolean cheese, int basePrice) {
        this.name = name;
        this.size = size;
        this.cheese = cheese;
        this.basePrice = basePrice;
    }
    
    @Override
    int getPrice() {
        int sizeCost = size == Size.MEDIUM ? 500 : size == Size.LARGE ? 1000 : 0;
        int cheeseCost = cheese ? 250 : 0;
        return basePrice + sizeCost + cheeseCost;
    }
}

class CustomizedPizza extends Pizza {
    String crust;
    String sauce;
    String topping;
    int basePrice = 3000;
    
    CustomizedPizza(String name, Size size, boolean cheese, String crust, String sauce, String topping) {
        this.name = name;
        this.size = size;
        this.cheese = cheese;
        this.crust = crust;
        this.sauce = sauce;
        this.topping = topping;
    }
    
    @Override
    int getPrice() {
        int cheeseCost = cheese ? 250 : 0;
        return basePrice + cheeseCost;
    }
}

class PizzaBuilder {
    String name;
    Size size;
    boolean cheese;
    String crust;
    String sauce;
    String topping;
    
    PizzaBuilder setName(String name) { this.name = name; return this; }
    PizzaBuilder setSize(Size size) { this.size = size; return this; }
    PizzaBuilder setCheese(boolean cheese) { this.cheese = cheese; return this; }
    PizzaBuilder setCrust(String crust) { this.crust = crust; return this; }
    PizzaBuilder setSauce(String sauce) { this.sauce = sauce; return this; }
    PizzaBuilder setTopping(String topping) { this.topping = topping; return this; }
    
    Pizza buildStandardPizza(int basePrice) {
        return new StandardPizza(name, size, cheese, basePrice);
    }
    
    Pizza buildCustomizedPizza() {
        return new CustomizedPizza(name, size, cheese, crust, sauce, topping);
    }
}

class DiscountStrategy {
    static int globalDiscount = 0;
    
    int applyDiscount(int price) {
        return price - (price * globalDiscount / 100);
    }
}


class Order {
    Pizza pizza;
    String status;
    String deliveryOption;
    String paymentMethod;
    String deliveryLocation;
    int price;
    
    Order(Pizza pizza, String deliveryOption, String paymentMethod, String deliveryLocation) {
        this.pizza = pizza;
        this.status = "Paid";
        this.deliveryOption = deliveryOption;
        this.paymentMethod = paymentMethod;
        this.deliveryLocation = deliveryLocation;
        this.price = new DiscountStrategy().applyDiscount(pizza.getPrice());
    }
    
    void updateStatus(String status) {
        this.status = status;
        JOptionPane.showMessageDialog(null, "Order status: " + status);
    }
}

interface OrderObserver {
    void update(Order order);
}

class User implements OrderObserver {
    String name;
    List<Order> orders = new ArrayList<>();
    
    User(String name) { this.name = name; }
    
    @Override
    public void update(Order order) {
        JOptionPane.showMessageDialog(null, "Order updated: " + order.status);
    }
    
    void placeOrder(Pizza pizza, String deliveryOption, String paymentMethod, String deliveryLocation) {
        Order order = new Order(pizza, deliveryOption, paymentMethod, deliveryLocation);
        orders.add(order);
        displayPriceBreakdown(order);
    }
    
    void displayPriceBreakdown(Order order) {
        int cheeseCost = order.pizza.cheese ? 250 : 0;
        int pizzaCost = order.pizza.getPrice() - cheeseCost;
        int discount = pizzaCost + cheeseCost - order.price;
        
        System.out.println("Order Number: " + (order.hashCode() & 0xfffffff));
        System.out.println("Cost for the Pizza: " + pizzaCost);
        System.out.println("Cost for the additional Cheese: " + cheeseCost);
        System.out.println("Discounts: " + discount);
        System.out.println("Final Amount to Pay: " + order.price);
    }
    
    void updateOrderStatus(Order order) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                order.updateStatus("Preparing");
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (order.deliveryOption.equals("Pickup")) {
                            order.updateStatus("Ready for picking");
                        } else {
                            order.updateStatus("Out for Delivery");
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    order.updateStatus("Delivered & ready for feedback");
                                }
                            }, 10 * 1000);  // 10 seconds interval
                        }
                    }
                }, 10 * 1000);  // 10 seconds interval
            }
        }, 10 * 1000);  // 10 seconds interval
    }
}


class Admin {
    Map<String, Integer> discounts = new HashMap<>();
    List<Order> feedback = new ArrayList<>();
    
    void defineDiscount(String name, int discount) {
        discounts.put(name, discount);
        DiscountStrategy.globalDiscount = discount;
    }
    
    void viewFeedback() {
        for (Order order : feedback) {
            System.out.println("Feedback: " + order.price + " | User: " + order.deliveryLocation);
        }
    }
}

class MapService {
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search?format=json&q=";
    private static final String OSRM_URL = "http://router.project-osrm.org/route/v1/driving/";
    
    // Method to get coordinates using Nominatim
    public static String[] getCoordinates(String location) throws Exception {
        URL url = new URL(NOMINATIM_URL + location.replace(" ", "%20"));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        // Parse JSON response to extract coordinates (latitude and longitude)
        // This is a simplified approach. You can use a JSON library like Jackson or Gson.
        String json = response.toString();
        String lat = json.split("\"lat\":\"")[1].split("\"")[0];
        String lon = json.split("\"lon\":\"")[1].split("\"")[0];
        return new String[] { lat, lon };
    }

    // Method to calculate the estimated delivery time using OSRM
    public static int getEstimatedDeliveryTime(String[] startCoordinates, String[] endCoordinates) throws Exception {
        String urlStr = OSRM_URL + startCoordinates[1] + "," + startCoordinates[0] + ";" 
        + endCoordinates[1] + "," + endCoordinates[0] + "?overview=false";
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        // Parse JSON response to extract duration (in seconds)
        String json = response.toString();
        String duration = json.split("\"duration\":")[1].split(",")[0];
        return (int) (Double.parseDouble(duration) / 60);  // Convert seconds to minutes
    }
}

public class PizzaOrderingSystem {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Admin admin = new Admin();
        Map<String, User> users = new HashMap<>();
        
        while (true) {
            System.out.println("1. Admin");
            System.out.println("2. User");
            int choice = sc.nextInt();
            
            if (choice == 1) {
                System.out.println("1. Define Discount");
                System.out.println("2. View Feedback");
                int adminChoice = sc.nextInt();
                
                if (adminChoice == 1) {
                    System.out.println("Enter discount name: ");
                    String discountName = sc.next();
                    System.out.println("Enter discount percentage: ");
                    int discount = sc.nextInt();
                    admin.defineDiscount(discountName, discount);
                } else if (adminChoice == 2) {
                    admin.viewFeedback();
                }
            } else if (choice == 2) {
                System.out.println("Enter user name: ");
                String userName = sc.next();
                User user = users.getOrDefault(userName, new User(userName));
                users.put(userName, user);
                
                // Display Admin Defined Discounts
                System.out.println("Current Discounts:");
                for (Map.Entry<String, Integer> entry : admin.discounts.entrySet()) {
                    System.out.println(entry.getKey() + ": " + entry.getValue() + "% off");
                }
                
                System.out.println("1. Order Pizza");
                System.out.println("2. View Customized Pizzas");
                int userChoice = sc.nextInt();
                
                if (userChoice == 1) {
                    System.out.println("1. Standard Pizza");
                    System.out.println("2. Customized Pizza");
                    int pizzaChoice = sc.nextInt();
                    
                    PizzaBuilder pizzaBuilder = new PizzaBuilder();
                    Pizza pizza = null;
                    if (pizzaChoice == 1) {
                        System.out.println("Select pizza name: ");
                        System.out.println("1. Pepperoni - 1500");
                        System.out.println("2. Chili Chicken - 1500");
                        System.out.println("3. Sausage Delight - 1600");
                        System.out.println("4. Chicken Bacon & Potato - 1700");
                        System.out.println("5. Veggie Supreme - 1200");
                        int pizzaNameChoice = sc.nextInt();
                        
                        String[] pizzaNames = { "Pepperoni", "Chili Chicken", "Sausage Delight", "Chicken Bacon & Potato", "Veggie Supreme" };
                        int[] pizzaPrices = { 1500, 1500, 1600, 1700, 1200 };
                        
                        String name = pizzaNames[pizzaNameChoice - 1];
                        int basePrice = pizzaPrices[pizzaNameChoice - 1];
                        
                        System.out.println("Select size: ");
                        System.out.println("1. Small - No extra cost");
                        System.out.println("2. Medium - Extra 500");
                        System.out.println("3. Large - Extra 1000");
                        int sizeChoice = sc.nextInt();
                        
                        Size size = Size.values()[sizeChoice - 1];
                        
                        System.out.println("Add cheese? (yes=1/no=0): Extra 250");
                        boolean cheese = sc.nextInt() == 1;
                        
                        pizza = pizzaBuilder.setName(name).setSize(size).setCheese(cheese).buildStandardPizza(basePrice);
                    } else {
                        System.out.println("Select crust: ");
                        System.out.println("1. Thin Crust");
                        System.out.println("2. Thick Crust");
                        System.out.println("3. Sausage Stuffed Crust");
                        String[] crusts = { "Thin Crust", "Thick Crust", "Sausage Stuffed Crust" };
                        int crustChoice = sc.nextInt();
                        
                        System.out.println("Select sauce: ");
                        System.out.println("1. Tomato Sauce");
                        System.out.println("2. Pesto Sauce");
                        System.out.println("3. BBQ Sauce");
                        String[] sauces = { "Tomato Sauce", "Pesto Sauce", "BBQ Sauce" };
                        int sauceChoice = sc.nextInt();
                        
                        System.out.println("Select topping: ");
                        System.out.println("1. Pepperoni");
                        System.out.println("2. Chili Chicken");
                        System.out.println("3. Sausage Delight");
                        System.out.println("4. Chicken Bacon & Potato");
                        System.out.println("5. Veggie Supreme");
                        String[] toppings = { "Pepperoni", "Chili Chicken", "Sausage Delight", "Chicken Bacon & Potato", "Veggie Supreme" };
                        int toppingChoice = sc.nextInt();
                        
                        System.out.println("Add cheese? (yes=1/no=0): Extra 250");
                        boolean cheese = sc.nextInt() == 1;
                        
                        System.out.println("Enter custom pizza name: ");
                        String customName = sc.next();
                        
                        pizza = pizzaBuilder.setName(customName).setSize(Size.SMALL).setCheese(cheese).setCrust(crusts[crustChoice - 1]).setSauce(sauces[sauceChoice - 1]).setTopping(toppings[toppingChoice - 1]).buildCustomizedPizza();
                    }
                    
                    System.out.println("Select delivery option: ");
                    System.out.println("1. Pickup");
                    System.out.println("2. Home Delivery");
                    String deliveryOption = sc.nextInt() == 1 ? "Pickup" : "Home Delivery";
                    
                    String deliveryLocation = "";
                    if (deliveryOption.equals("Home Delivery")) {
                        System.out.println("Enter delivery location: ");
                        deliveryLocation = sc.next();
                        
                        // Use MapService to get coordinates and estimated delivery time
                        try {
                            String[] shopCoordinates = { "6.9388614", "79.8542005" };  // Updated shop coordinates
                            String[] userCoordinates = MapService.getCoordinates(deliveryLocation);
                            int estimatedTime = MapService.getEstimatedDeliveryTime(shopCoordinates, userCoordinates);
                            System.out.println("Estimated delivery time: " + estimatedTime + " minutes");
                        } catch (Exception e) {
                            System.out.println("Error: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    
                    user.placeOrder(pizza, deliveryOption, "", deliveryLocation);  // Empty payment method for now
                    
                    System.out.println("Select payment method: ");
                    System.out.println("1. Credit card");
                    System.out.println("Select payment method: ");
                    System.out.println("1. Credit card");
                    System.out.println("2. Debit card");
                    System.out.println("3. LankaPay");
                    String[] paymentMethods = { "Credit card", "Debit card", "LankaPay" };
                    int paymentChoice = sc.nextInt();
                    String paymentMethod = paymentMethods[paymentChoice - 1];
                    
                    System.out.println("Enter card/account number: ");
                    String cardNumber = sc.next();
                    
                    // Update order status to "Paid"
                    Order lastOrder = user.orders.get(user.orders.size() - 1);
                    lastOrder.paymentMethod = paymentMethod;
                    lastOrder.updateStatus("Paid");
                    
                    // Update order status to "Preparing"
                    user.updateOrderStatus(lastOrder);
                    
                    System.out.println("Order placed successfully!");
                    System.out.println("Total Price: " + lastOrder.price);
                    System.out.println("Payment method: " + paymentMethod);
                } else if (userChoice == 2) {
                    System.out.println("Customized Pizzas:");
                    int i = 1;
                    for (Order order : user.orders) {
                        if (order.pizza instanceof CustomizedPizza) {
                            System.out.println(i + ". " + order.pizza.name);
                            i++;
                        }
                    }
                    
                    System.out.println("Select a customized pizza to reorder (enter number): ");
                    int reorderChoice = sc.nextInt();
                    if (reorderChoice <= user.orders.size() && user.orders.get(reorderChoice - 1).pizza instanceof CustomizedPizza) {
                        Pizza pizza = user.orders.get(reorderChoice - 1).pizza;
                        System.out.println("Reordering pizza: " + pizza.name);
                        user.placeOrder(pizza, "Pickup", "Credit Card", "SampleLocation");  // For quick testing.
                    }
                }
            }
        }
    }
}