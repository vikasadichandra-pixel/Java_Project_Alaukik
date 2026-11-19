"use strict";

const $ = (selector) => document.querySelector(selector);
const searchForm = $("#search-form");
const checkoutForm = $("#checkout-form");
const customerSelect = $("#customer");
const cart = new Map();
const currency = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" });
const money = (amount) => currency.format(amount);
let loadingCheckout = false;
let latestSearch = 0;
let latestHistory = 0;

function element(tag, text = "", className = "") {
    const node = document.createElement(tag);
    node.textContent = text;
    node.className = className;
    return node;
}

async function request(path, body) {
    const options = body === undefined ? undefined : {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    };
    const response = await fetch("/api" + path, options);
    const result = await response.json();
    if (!response.ok) {
        throw new Error(result.message || `Request failed (${response.status}).`);
    }
    return result;
}

function showStatus(message, type = "") {
    const status = $("#status");
    status.textContent = message;
    status.className = type;
}

async function showProducts() {
    const sequence = ++latestSearch;
    const query = new FormData(searchForm).get("q");
    const products = await request("/products?q=" + encodeURIComponent(query));
    if (sequence !== latestSearch) return;
    const grid = $("#products");
    grid.replaceChildren();
    $("#product-count").textContent = `${products.length} found`;
    if (!products.length) grid.append(element("p", "No products match your search.", "empty"));

    for (const product of products) {
        const card = element("article", "", "product");
        const meta = element("p", `${product.category} · ${product.stock} in stock`, "meta");
        const footer = element("div", "", "product-foot");
        const add = element("button", "Add");
        add.setAttribute("aria-label", "Add " + product.name);
        add.disabled = product.stock === 0;
        add.addEventListener("click", () => {
            if (loadingCheckout) return;
            const previous = cart.get(product.id)?.quantity ?? 0;
            cart.set(product.id, { product, quantity: Math.min(99, previous + 1) });
            renderCart();
        });
        footer.append(element("strong", money(product.price)), add);
        card.append(element("h3", product.name), meta, footer);
        grid.append(card);
    }
}

function renderCart() {
    const lines = $("#cart-lines");
    lines.replaceChildren();
    let subtotal = 0;
    let count = 0;
    for (const [id, item] of cart) {
        const row = element("div", "", "line");
        const quantity = element("input");
        quantity.type = "number";
        quantity.min = "1";
        quantity.max = "99";
        quantity.value = item.quantity;
        quantity.disabled = loadingCheckout;
        quantity.setAttribute("aria-label", "Quantity for " + item.product.name);
        quantity.addEventListener("change", () => {
            item.quantity = Math.min(99, Math.max(1, Math.trunc(Number(quantity.value) || 1)));
            renderCart();
        });
        const remove = element("button", "Remove", "secondary");
        remove.disabled = loadingCheckout;
        remove.setAttribute("aria-label", "Remove " + item.product.name);
        remove.addEventListener("click", () => {
            cart.delete(id);
            renderCart();
        });
        row.append(element("span", item.product.name), quantity, remove);
        lines.append(row);
        subtotal += item.quantity * Math.round(item.product.price * 100);
        count += item.quantity;
    }
    if (!count) lines.append(element("p", "Your cart is empty.", "empty"));
    $("#cart-count").textContent = count === 1 ? "1 item" : `${count} items`;
    $("#subtotal").textContent = money(subtotal / 100);
    $("#place-order").disabled = loadingCheckout || cart.size === 0 || !customerSelect.value;
}

async function showCustomers(selected = customerSelect.value) {
    const customers = await request("/customers");
    customerSelect.replaceChildren();
    for (const customer of customers) {
        const option = element("option", customer.name + " (" + customer.email + ")");
        option.value = customer.id;
        customerSelect.append(option);
    }
    if (selected) customerSelect.value = selected;
    renderCart();
    await showOrders();
}

async function showOrders() {
    const sequence = ++latestHistory;
    const customerId = customerSelect.value;
    const orders = customerId ? await request("/orders?customerId=" + customerId) : [];
    if (sequence !== latestHistory) return;
    const history = $("#orders");
    history.replaceChildren();
    if (!orders.length) history.append(element("p", "No orders for this customer.", "empty"));
    for (const order of orders) {
        const row = element("div", "", "order");
        row.append(element("span", new Date(order.placedAt).toLocaleString("en-IN")),
            element("strong", money(order.total)), element("small", order.id));
        history.append(row);
    }
}

searchForm.addEventListener("submit", (event) => {
    event.preventDefault();
    showProducts().catch((error) => showStatus(error.message, "error"));
});
customerSelect.addEventListener("change", () => {
    $("#receipt").replaceChildren();
    showStatus("");
    showOrders().catch((error) => showStatus(error.message, "error"));
});

checkoutForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (loadingCheckout || !cart.size) return;
    const form = new FormData(checkoutForm);
    loadingCheckout = true;
    customerSelect.disabled = true;
    renderCart();
    showStatus("");
    $("#receipt").replaceChildren();
    try {
        const receipt = await request("/checkout", {
            customerId: Number(form.get("customerId")),
            items: [...cart].map(([productId, item]) => ({ productId, quantity: item.quantity })),
            coupon: form.get("coupon")
        });
        cart.clear();
        showStatus("Order placed.", "success");
        $("#receipt").append(element("strong", "Order " + receipt.id),
            element("p", "Subtotal " + money(receipt.subtotal)),
            element("p", "Discount " + money(receipt.discount)),
            element("strong", "Total " + money(receipt.total)));
    } catch (error) {
        showStatus(error.message, "error");
    } finally {
        customerSelect.disabled = false;
        loadingCheckout = false;
        renderCart();
        const refresh = await Promise.allSettled([showProducts(), showOrders()]);
        if (refresh.some((result) => result.status === "rejected")) {
            $("#status").append(element("p", "Could not refresh the catalog or order history."));
        }
    }
});

$("#registration").addEventListener("submit", async (event) => {
    event.preventDefault();
    if (loadingCheckout) return;
    const form = event.currentTarget;
    const status = $("#registration-status");
    try {
        const customer = await request("/customers", Object.fromEntries(new FormData(form)));
        form.reset();
        status.textContent = "Customer registered.";
        status.className = "success";
        await showCustomers(String(customer.id));
    } catch (error) {
        status.textContent = error.message;
        status.className = "error";
    }
});

renderCart();
Promise.all([showProducts(), showCustomers()]).catch((error) => showStatus(error.message, "error"));

