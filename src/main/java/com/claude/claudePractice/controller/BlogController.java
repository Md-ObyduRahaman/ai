package com.claude.claudePractice.controller;

import com.claude.claudePractice.model.BlogPost;
import com.claude.claudePractice.model.CartItemEntity;
import com.claude.claudePractice.repository.CartItemRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@Controller
public class BlogController {

    private final CartItemRepository cartItemRepository;

    public BlogController(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    private int getCartCount() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        if (username.equals("anonymousUser")) return 0;
        return cartItemRepository.findByUsername(username)
            .stream().mapToInt(CartItemEntity::getQuantity).sum();
    }

    private List<BlogPost> getBlogPosts() {
        return List.of(
            new BlogPost(1,
                "Top 10 Tech Gadgets to Watch in 2026",
                """
                <p>The tech landscape is evolving faster than ever. From AI-powered assistants to next-gen wearables, 2026 is shaping up to be a groundbreaking year for consumer electronics.</p>

                <h3>1. AI-Integrated Smartwatches</h3>
                <p>Modern smartwatches are no longer just step counters. With on-device AI processing, they now offer real-time health diagnostics, stress monitoring, and even sleep apnea detection.</p>

                <h3>2. Wireless Noise-Canceling Earbuds</h3>
                <p>The latest earbuds feature adaptive noise cancellation that adjusts to your environment automatically. Battery life has also seen a massive leap, with some models lasting up to 12 hours on a single charge.</p>

                <h3>3. Portable Bluetooth Speakers</h3>
                <p>Portable speakers now pack room-filling sound in a pocket-sized form factor. With IP67 water resistance and 360-degree audio, they're the perfect companion for any adventure.</p>

                <h3>4. Ultrabook Laptops</h3>
                <p>The new generation of ultrabooks combines stunning performance with all-day battery life. With AI-powered cooling and轻薄 designs, they're redefining what a laptop can do.</p>

                <h3>5. Digital Cameras with AI Framing</h3>
                <p>AI-powered autofocus and composition assistance make professional-quality photography accessible to everyone. Say goodbye to blurry shots and poorly framed photos.</p>

                <p>These are just a few highlights — the tech world is brimming with innovation. Stay tuned for more updates!</p>
                """,
                "From AI-powered wearables to next-gen audio, explore the most exciting tech innovations hitting the market this year.",
                "Sarah Mitchell", "/images/laptop.svg", LocalDate.of(2026, 6, 28)),

            new BlogPost(2,
                "How to Choose the Perfect Wireless Headphones",
                """
                <p>With so many options on the market, finding the right pair of wireless headphones can feel overwhelming. Here's our comprehensive guide to making the right choice.</p>

                <h3>Consider Your Use Case</h3>
                <p>Are you a commuter who needs noise cancellation? A home-office worker looking for comfort during long calls? Or a fitness enthusiast needing sweat-resistant buds? Your primary use case should drive your decision.</p>

                <h3>Sound Quality Matters</h3>
                <p>Look for headphones with aptX HD or LDAC support for high-resolution audio. Don't be fooled by bass-heavy tuning — the best headphones offer a balanced sound profile across all frequencies.</p>

                <h3>Battery Life</h3>
                <p>Aim for at least 20 hours of playback time for over-ear headphones and 6+ hours for true wireless earbuds. Fast charging is a must-have feature in 2026.</p>

                <h3>Comfort and Build</h3>
                <p>Try before you buy — or check reviews that discuss comfort for extended wear. Memory foam ear cushions and lightweight materials make a huge difference during long listening sessions.</p>

                <br/><p>Remember, the best headphones are the ones that fit your lifestyle and budget. Happy listening!</p>
                """,
                "Shopping for wireless headphones? Our guide breaks down everything from sound quality to battery life so you can make the right choice.",
                "Alex Rivera", "/images/headphones.svg", LocalDate.of(2026, 6, 21)),

            new BlogPost(3,
                "Smart Home Setup Guide for Beginners",
                """
                <p>Building a smart home doesn't have to be complicated or expensive. Follow this step-by-step guide to create a connected home that works for you.</p>

                <h3>Start with a Smart Speaker</h3>
                <p>Your smart home needs a brain. A smart speaker or display acts as your central hub, letting you control lights, thermostats, and more with voice commands.</p>

                <h3>Smart Lighting</h3>
                <p>Smart bulbs are the easiest entry point. Start with one or two for your most-used rooms. Look for bulbs that support color temperature adjustment to match your natural circadian rhythm.</p>

                <h3>Smart Plugs</h3>
                <p>These inexpensive devices turn any appliance into a smart device. Schedule your coffee maker to start in the morning or turn off lamps remotely when you're on vacation.</p>

                <h3>Security Cameras</h3>
                <p>A single doorbell camera adds peace of mind. Modern options include AI-powered person detection and package alerts, so you'll never miss a delivery again.</p>

                <br/><p>Remember: start small, add gradually, and choose devices that work with a single ecosystem for the smoothest experience.</p>
                """,
                "New to smart home tech? Start here — we'll walk you through the essentials from smart speakers to security cameras.",
                "Jason Lee", "/images/watch.svg", LocalDate.of(2026, 6, 14)),

            new BlogPost(4,
                "The Ultimate Guide to USB-C: One Cable to Rule Them All",
                """
                <p>USB-C has become the universal standard for charging and data transfer. Here's everything you need to know about this versatile technology.</p>

                <h3>What Makes USB-C Special?</h3>
                <p>Unlike its predecessors, USB-C is reversible, supports high-speed data transfer (up to 40Gbps with Thunderbolt 4), and can deliver up to 240W of power — enough to charge even the largest laptops.</p>

                <h3>USB-C vs. Other Connectors</h3>
                <p>Gone are the days of carrying multiple cables. USB-C can replace HDMI, DisplayPort, USB-A, and even power cables. Many modern laptops only have USB-C ports, relying on dongles for legacy connections.</p>

                <h3>What to Look For When Buying</h3>
                <p>Not all USB-C cables are created equal. Check for USB-IF certification, look at the data transfer speed rating, and ensure the cable supports the power delivery wattage your devices need.</p>

                <br/><p>Invest in quality cables — they'll last longer, charge faster, and transfer data at full speed.</p>
                """,
                "USB-C is everywhere, but not all cables are equal. Learn what to look for and why this connector is taking over the tech world.",
                "Sarah Mitchell", "/images/headphones.svg", LocalDate.of(2026, 6, 7)),

            new BlogPost(5,
                "Photography Tips for Capturing Stunning Travel Photos",
                """
                <p>Whether you're using a DSLR or a smartphone, these tips will help you take travel photos that tell a story and preserve your memories beautifully.</p>

                <h3>Golden Hour is Your Best Friend</h3>
                <p>The hour after sunrise and the hour before sunset offer soft, warm light that makes any subject look amazing. Plan your most important shots around these times.</p>

                <h3>Rule of Thirds</h3>
                <p>Enable grid lines on your camera or phone and place your subject at the intersection points. This simple technique instantly improves composition and visual interest.</p>

                <h3>Tell a Story</h3>
                <p>Instead of just shooting landmarks, capture the details: street food being cooked, local artisans at work, interesting textures and patterns. These shots bring your travel album to life.</p>

                <h3>Invest in a Good Camera</h3>
                <p>A mirrorless camera or a high-end point-and-shoot offers significantly better image quality than most smartphones, especially in low light. Look for features like in-body stabilization and RAW support for maximum flexibility.</p>

                <br/><p>The best camera is the one you have with you — but having the right one helps!</p>
                """,
                "Elevate your travel photography with these practical tips, from mastering golden hour light to composing shots that tell a story.",
                "Jason Lee", "/images/camera.svg", LocalDate.of(2026, 5, 31)),

            new BlogPost(6,
                "Tablet vs Laptop: Which One Do You Really Need?",
                """
                <p>With tablets becoming increasingly powerful, the line between tablets and laptops has blurred. Here's how to decide which device fits your needs.</p>

                <h3>Content Consumption</h3>
                <p>If you primarily watch videos, read e-books, browse social media, or play casual games, a tablet is the better choice. It's lighter, more portable, and offers a superior touch experience.</p>

                <h3>Productivity and Work</h3>
                <p>For coding, writing lengthy documents, editing video, or any task requiring heavy multitasking, a laptop remains essential. Even the best tablets can't fully replace a laptop for professional work.</p>

                <h3>The Best of Both Worlds</h3>
                <p>Consider a convertible laptop or a tablet with a quality keyboard attachment. This gives you the flexibility of a tablet for casual use with the productivity power of a laptop when you need it.</p>

                <h3>Our Recommendation</h3>
                <p>If you can only afford one device and do any kind of work on it, choose a laptop. If you already have a capable desktop, a tablet makes an excellent companion device.</p>

                <br/><p>Ultimately, the best device is the one that matches your lifestyle — and sometimes, having both is the real answer!</p>
                """,
                "Can't decide between a tablet and a laptop? We break down the pros and cons to help you choose the right device for your lifestyle.",
                "Alex Rivera", "/images/tablet.svg", LocalDate.of(2026, 5, 24))
        );
    }

    @GetMapping("/blog")
    public String blogList(Model model) {
        model.addAttribute("pageTitle", "Blog - ShopEasy");
        model.addAttribute("brandName", "ShopEasy");
        model.addAttribute("currentYear", Year.now().getValue());
        model.addAttribute("cartCount", getCartCount());

        model.addAttribute("posts", getBlogPosts());
        model.addAttribute("recentPosts", getBlogPosts().subList(0, Math.min(3, getBlogPosts().size())));
        return "blog";
    }

    @GetMapping("/blog/{id}")
    public String blogPost(@PathVariable int id, Model model) {
        var post = getBlogPosts().stream()
            .filter(p -> p.getId() == id)
            .findFirst()
            .orElse(null);

        if (post == null) {
            return "redirect:/blog";
        }

        model.addAttribute("pageTitle", post.getTitle() + " - ShopEasy");
        model.addAttribute("brandName", "ShopEasy");
        model.addAttribute("currentYear", Year.now().getValue());
        model.addAttribute("cartCount", getCartCount());

        model.addAttribute("post", post);
        model.addAttribute("recentPosts", getBlogPosts().subList(0, Math.min(3, getBlogPosts().size())));
        return "blog-post";
    }
}
