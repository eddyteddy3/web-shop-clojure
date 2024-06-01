(ns custom-web-shop-app.core
  (:require [clojure.pprint]
            ;; [custom-web-shop-app.input-simple :as input]
            [custom-web-shop-app.input-random :as input]
            ))

; Logging
(def logger (agent nil))
(defn log [& msgs] (send logger (fn [_] (apply println msgs)))) ; uncomment this to turn ON logging
;; (defn log [& msgs] nil) ; uncomment this to turn OFF logging

;; (defn foo
;;   "I don't do a whole lot."
;;   [x]
;;   (do (println input/products)
;;       (println x " hehe")))

; ----------- Domain Models -------------

; We simply copy the products from the input file, without modifying them.
;; products are defined in input.random file (input name space)
(def products input/products)

; We wrap the stock from the input file in a single atom in this
; implementation. You are free to change this to a more appropriate mechanism.
(def stock (atom input/stock))

(defn product-name->id [name]
  "Return id (= index) of product with given `name`.
  E.g. (product-name->id \"Apple\") = 0"
  (.indexOf products name))

(defn product-id->name [id]
  "Return name of product with given `id`.
  E.g. (product-id->name 0) = \"Apple\""
  (nth products id))


; We simply copy the stores from the input file, without modifying them.
(def stores input/stores)

;; add a condition where the index doesn't fall below -1!! (it goes for every index (int))
(defn store-name->id [name]
  "Return id (= index) of store with given `name`.
  E.g. (store-name->id \"Aldi\") = 0"
  (.indexOf stores name))

(defn store-id->name [id]
  "Return name of store with given `id`.
  E.g. (store-id->name 0) = \"Aldi\""
  (nth stores id))


; We wrap the prices from the input file in a single atom in this
; implementation. You are free to change this to a more appropriate mechanism.
(def prices (atom input/prices))

(defn get-price [store-id product-id]
  "Returns the price of the given product in the given store."
  (nth (nth @prices product-id) store-id))

(defn get-current-stock-of-product-at-store
  [product-id store-id]
  (nth (nth @stock product-id) store-id)
  )

(defn get-current-price-of-product-at-store
  [product-id store-id]
  (nth (nth @prices product-id) store-id)
  )

(defn get-total-price [store-id product-ids-and-number]
  "Returns the total price for a given number of products in the given store."
  (reduce +
    (map
      (fn [[product-id n]]
        (* n (get-price store-id product-id)))
      product-ids-and-number)))

(defn set-price [store-id product-id new-price]
  "Set the price of the given product in the given store to `new-price`."
  (swap! prices assoc-in [product-id store-id] new-price))

(defn print-stock [stock]
  "Print stock. Note: `stock` should not be an atom/ref/... but the value it
  contains."
  (println "Stock:")
  ; Print header row with store names (abbreviated to four characters)
  (doseq [store stores]
    (print (apply str (take 4 store)) ""))
  (println)
  ; Print table
  (doseq [product-id (range (count stock))]
    ; Line of numbers
    (doseq [number-in-stock (nth stock product-id)]
      (print (clojure.pprint/cl-format nil "~4d " number-in-stock)))
    ; Name of product
    (println (product-id->name product-id))))

(defn product-available? [store-id product-id n]
  "Returns true if at least `n` of the given product are still available in the
  given store."
  (>= (nth (nth @stock product-id) store-id) n))

(defn find-available-stores [product-ids-and-number]
  "Returns the id's of the stores in which the given products are still
  available."
  (filter
    (fn [store-id]
      (every?
        (fn [[product-id n]] (product-available? store-id product-id n))
        product-ids-and-number))
    (map store-name->id stores)))

(defn buy-product [store-id product-id n]
  "Updates `stock` to buy `n` of the given product in the given store."
  ;; (swap! stock
  ;; using alter instead
  (swap! stock
         (fn [old-stock]
           (update-in old-stock [product-id store-id]
                      (fn [available] (- available n))))))

(defn buy-products [store-id product-ids-and-number]
  (doseq [[product-id n] product-ids-and-number]
    (buy-product store-id product-id n)))

;; (defn extract-product-id-and-quantity
;;   [product-name quantity]
;;   ((product-name->id product-name) quantity))

(defn get-customer-products-with-id-and-quantity
  "returns the customer's product with product's quantity and ID"
  [customer]
  ;; it would have been nice although, if i would be able to extract
  ;; the following anon function to its own function like SWIFT
  (map (fn [[name number]] [(product-name->id name) number]) ;;(extract-product-id-and-quantity name 1)
       (:products customer)))

;; sequential processing of customer
(defn process-customer [customer]
  "Process `customer`. Consists of three steps:
  1. Finding all stores in which the requested products are still available.
  2. Sorting the found stores to find the cheapest (for the sum of all products).
  3. Buying the products by updating the `stock`.

  Note: because this implementation is sequential, we do not suffer from
  inconsistencies. That will be different in your implementation."
  (let [
        product-ids-and-number ;; 
        (get-customer-products-with-id-and-quantity customer)
        ;; (map (fn [[name number]] [(product-name->id name) number])
        ;;      (:products customer))

        available-store-ids  ; step 1
        (find-available-stores product-ids-and-number)

        cheapest-store-id  ; step 2
        (first  ; Returns nil if there's no available stores
         (sort-by
              ; sort stores by total price
          (fn [store-id] (get-total-price store-id product-ids-and-number))
          available-store-ids))]

    (if (nil? cheapest-store-id)
      ;; (log "Customer" (:id customer) "could not find a store that has"
      ;;   (:products customer))
      nil
      (do
        (buy-products cheapest-store-id product-ids-and-number) ;  step 3
        ;; (log "Customer" (:id customer) "bought" (:products customer) "in"
        ;;   (store-id->name cheapest-store-id))
        ))))

(def finished-processing?
  "Set to true once all customers have been processed, so that sales process
  can end."
  (atom false))

(defn process-customers [customers]
  "Process `customers` one by one. In this code, this happens sequentially. In
  your implementation, this should be parallelized."
  (doseq [customer customers]
    (process-customer customer))
  (reset! finished-processing? true))

(defn start-sale [store-id]
  "Sale: -10% on `store-id`."
  (log "Start sale for store" (store-id->name store-id))
  (doseq [product-id (range (count products))]
    (set-price store-id product-id (* (get-price store-id product-id) 0.90))))

(defn end-sale [store-id]
  "End sale: reverse discount on `store-id`."
  (log "End sale for store" (store-id->name store-id))
  (doseq [product-id (range (count products))]
    (set-price store-id product-id (/ (get-price store-id product-id) 0.90))))

(defn sales-process []
  "The sales process starts and ends sales periods, until `finished-processing?`
  is true."
  (loop []
    (let [store-id (store-name->id (rand-nth stores))]
      (Thread/sleep input/TIME_BETWEEN_SALES)
      (start-sale store-id)
      (Thread/sleep input/TIME_OF_SALES)
      (end-sale store-id))
    (if (not @finished-processing?)
      (recur))))

(defn sort-cart-by-price
  
  [product-id-and-quantity available-store-ids]
  
  (sort-by
   (fn [store-id] (get-total-price store-id product-id-and-quantity))
   available-store-ids)
  )

(defn simulate-purchase
  "just a test code for experimental purposes"
  [customer]
  ;; (println "before do sync")
  ;; (println "customer's products " (get-customer-products-with-id-and-quantity customer))
  ;; (println "available stores " (find-available-stores (get-customer-products-with-id-and-quantity customer)))
  (let [
        ;; getting product ids and numbers
        product-ids-and-number (get-customer-products-with-id-and-quantity customer)

        ;; finding available stores using the IDs along with quantity
        available-store-ids (find-available-stores (get-customer-products-with-id-and-quantity customer))
        
        ;; finding cheapest store 
        cheapest-store-id  ; step 2
        (if (empty? available-store-ids)
          nil
          (first  ; Returns nil if there's no available stores
           (sort-cart-by-price product-ids-and-number available-store-ids)

          ;;  (sort-by
          ;;                                   ; sort stores by total price
          ;;   (fn [store-id] (get-total-price store-id product-ids-and-number))
          ;;   available-store-ids)
           ))
        
        ]
    ;; (println "cheapest store " cheapest-store-id)

    (if (nil? cheapest-store-id)
      ;; (log "Customer" (:id customer) "could not find a store that has"
          ;;  (:products customer))
      nil

      (do
        ;; (log "processing Customer" (:id customer))
        ;; (println "stock before " (deref stock))
        ;; breaking down `product-ids-and-number` into `prod-id` & `quantity`
        (doseq [[prod-id quantity] product-ids-and-number]
          ;; (println "id " prod-id)
          ;; (println "quanity " quantity)
          

          (buy-product cheapest-store-id prod-id quantity) 
          ;; (println "")
          )
        ;; (println "stock after " (deref stock))
        ;; (println "")
          ;; (
          ;;  println "after stock update" (update-in @stock [prod-id cheapest-store-id] (fn [current] (- current quantity))))
          ;; )
        
        ;; (println "Purchase made, new stock:" (nth (nth @stock 0) cheapest-store-id))
        ;; (println "id and number" product-ids-and-number)
        ;; (buy-products cheapest-store-id product-ids-and-number)
        ;; (log "Customer" (:id customer) "bought" (:products customer) "in"
            ;;  (store-id->name cheapest-store-id)))
        )
      )
    ;; (println "")

    )
  ;; (dosync)
  ;; (dosync
  ;;  (let [current-stock (@stock product-id)]
  ;;    (println "current stock " current-stock)
  ;;    (println "quanity " quantity)
  ;;    (when (>= current-stock quantity)
  ;;      (let [remain-stock (alter current-stock - quantity)]
  ;;        (println "remaining stock " remain-stock))
  ;;      )))
  )

(defn simulate-purchase-concurrent
  "just a test code for experimental purposes for concurrency"
  [customer]
  (let [;; getting product ids and numbers
        product-ids-and-number (get-customer-products-with-id-and-quantity customer)

        ;; finding available stores using the IDs along with quantity
        available-store-ids (find-available-stores (get-customer-products-with-id-and-quantity customer))

        ;; finding cheapest store 
        cheapest-store-id
        (if (empty? available-store-ids)
          nil
          (first  ; Returns nil if there's no available stores
           (sort-cart-by-price product-ids-and-number available-store-ids)

          ;;  (sort-by
          ;;                                   ; sort stores by total price
          ;;   (fn [store-id] (get-total-price store-id product-ids-and-number))
          ;;   available-store-ids)
           ))]
    ;; (println "cheapest store " cheapest-store-id)

    (if (nil? cheapest-store-id)
      ;; (log "Customer" (:id customer) "could not find a store that has"
          ;;  (:products customer))
      nil

      (do
        ;; (log "processing Customer" (:id customer))
        ;; (println "stock before " (deref stock))
        ;; breaking down `product-ids-and-number` into `prod-id` & `quantity`
        (doseq [[prod-id quantity] product-ids-and-number]
          ;; (println "id " prod-id)
          ;; (println "quanity " quantity)


          (buy-product cheapest-store-id prod-id quantity)
          ;; (println "")
          )
        ;; (println "stock after " (deref stock))
        ;; (println "")
          ;; (
          ;;  println "after stock update" (update-in @stock [prod-id cheapest-store-id] (fn [current] (- current quantity))))
          ;; )

        ;; (println "Purchase made, new stock:" (nth (nth @stock 0) cheapest-store-id))
        ;; (println "id and number" product-ids-and-number)
        ;; (buy-products cheapest-store-id product-ids-and-number)
        ;; (log "Customer" (:id customer) "bought" (:products customer) "in"
            ;;  (store-id->name cheapest-store-id)))
        ))
    ;; (println "")
    )
  ;; (dosync)
  ;; (dosync
  ;;  (let [current-stock (@stock product-id)]
  ;;    (println "current stock " current-stock)
  ;;    (println "quanity " quantity)
  ;;    (when (>= current-stock quantity)
  ;;      (let [remain-stock (alter current-stock - quantity)]
  ;;        (println "remaining stock " remain-stock))
  ;;      )))
  )

(def temp-customer-1 (nth input/customers 1))
(def temp-customer-2 (nth input/customers 2))

;;3rd is a heavy shopper
(def temp-customer-3 {:id 3 :products [["Banana" 9] ["Avocado" 10] ["Cherry" 6]]})

(defn run-sim []
  (let [initial-stock [10 10 10]  ; assume three products with 10 units each
        ]
    (do
      (time
      (doseq [customer input/customers]
        (simulate-purchase customer)
        )
      )
      ;; (println "stock before " (deref stock))
      ;; (simulate-purchase temp-customer-1)
      ;; (simulate-purchase temp-customer-2)
      ;; (simulate-purchase temp-customer-3)
      ;; (println "Final Stock:" @stock)
      )
    ;; (println "Initial Stock:" @stock)
    ;; (future (simulate-purchase temp-customer-1))  ; purchase 5 units of product 0
    ;; (future (simulate-purchase temp-customer-2))  ; attempt to purchase 6 units of product 0
    ;; (println "Final Stock:" @stock)))
  ))

;; (defn simulate-purchase
;;   "Simulate a purchase transaction on a given product."
;;   [product-id store-id quantity]  ; Added `store-id` for clarity in accessing specific stock.
;;   (dosync
;;    (let [product-stock (nth @stock product-id)  ; Get stock vector for the specific product.
;;          current-stock (nth product-stock store-id)]  ; Get stock for the product at a specific store.
;;      (println "Attempting to purchase," "product-id:" product-id, "store-id:" store-id, "current stock:" current-stock, "quantity:" quantity)
;;      (when (>= current-stock quantity)  ; Check if the stock is sufficient.
;;        (println "Purchase possible, proceeding.")
;;        (alter stock update-in [product-id store-id] #(- % quantity))
;;        (println "Purchase made, new stock:" (nth (nth @stock product-id) store-id)))
;;      (println "Exiting transaction"))))

;; (defn run-simulation []
  ;; (println "Initial Stock:" @stock)
  ;; (let [f1 (future (simulate-purchase 0 0 5))  ; Assuming store-id 0, product-id 0
  ;;       f2 (future (simulate-purchase 0 0 6))]  ; Both purchases for the same product and store
  ;;   (deref f1)
  ;;   (deref f2)
  ;;   (println "Final Stock after all purchases:" @stock)))



(defn main []
  ; Print parameters
  ;; (println "Number of products:" (count products))
  ;; (println "Number of stores:" (count stores))
  ;; (println "Number of customers:" (count input/customers))
  ;; (println "Time between sales:" input/TIME_BETWEEN_SALES)
  ;; (println "Time of sales:" input/TIME_OF_SALES)
  ;; ; Print initial stock
  ;; (println "Initial stock:")
  ;; (print-stock @stock)
  ; Start two threads: one for processing customers, one for sales.
  ; Print the time to execute the first thread.
  ;; (let [f1 (future (time (process-customers input/customers)))
  ;;       f2 (future (sales-process))]
  ;;   ; Wait until both have finished
  ;;   @f1
  ;;   @f2
  ;;   (await logger))
  ; Print final stock, for manual verification and debugging
  (time (process-customers input/customers))
  ;; (println "Final stock:")
  ;; (print-stock @stock)
)

;; (main)
;; (shutdown-agents)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (do
    ;; (main)
    (run-sim)
    (shutdown-agents))
  ;; (if (= *ns* 'clojure.main)
  ;;   )
  ;; (do (println "Hello, World!")
  ;;      (run-sim)
  ;;      (shutdown-agents)
  ;;     ;;  (foo "Hello")
  ;;     )
  )

  ;; (do (println "Hello, World!")
  ;;     (main)
  ;;     (shutdown-agents)
  ;;     (foo "Hello")))


;;

;lein run (sequential)
; "Elapsed time: 10761.226333 msecs" ;