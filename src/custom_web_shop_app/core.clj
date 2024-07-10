(ns custom-web-shop-app.core
  (:require [clojure.pprint]

            ;; [custom-web-shop-app.insufficient-store-test-case :as input]            

            ;; [custom-web-shop-app.input-random :as input]

            [custom-web-shop-app.input-simple :as input]

            ;; [custom-web-shop-app.student-test-case :as input]
            ))

(import '(java.util.concurrent Executors))
(require '[metrics.core :refer [new-registry]])
(require '[metrics.counters :refer [counter]])
(require '[metrics.counters :refer [value]])
(require '[metrics.counters :refer [inc!]])

;; https://stackoverflow.com/questions/5397955/sleeping-a-thread-inside-an-executorservice-java-clojure
;; https://stackoverflow.com/questions/41284533/concurrent-process-in-clojure
;; https://github.com/clj-commons/claypoole
(def executor-service (Executors/newFixedThreadPool 50))

; ------- Misc --------

; Logging
;; (def logger (agent nil))
;; (defn log [& msgs] (send logger (fn [_] (apply println msgs)))) ; uncomment this to turn ON logging
;; (defn log [& msgs] nil) ; uncomment this to turn OFF logging

;; (defn foo
;;   "I don't do a whole lot."
;;   [x]
;;   (do (println input/products)
;;       (println x " hehe")))

; ----------- Data models -------------

(def products input/products)
;; (def stock (atom input/stock))
(def stock (ref input/stock))
; We simply copy the stores from the input file, without modifying them.
(def stores input/stores)
;; (def prices (ref input/prices))
(def prices (atom input/prices))

(defn product-name->id [name]
  "Return id (= index) of product with given `name`.
  E.g. (product-name->id \"Apple\") = 0"
  (.indexOf products name))

(defn product-id->name [id]
  "Return name of product with given `id`.
  E.g. (product-id->name 0) = \"Apple\""
  (nth products id))

;; add a condition where the index doesn't fall below -1!! (it goes for every index (int))
(defn store-name->id [name]
  "Return id (= index) of store with given `name`.
  E.g. (store-name->id \"Aldi\") = 0"
  (.indexOf stores name))

(defn get-price [store-id product-id]
  "Returns the price of the given product in the given store."
  (nth (nth @prices product-id) store-id))

(defn set-price [store-id product-id new-price]
  "Set the price of the given product in the given store to `new-price`."
  (swap! prices assoc-in [product-id store-id] new-price)
  ;; (dosync alter prices assoc-in [product-id store-id] new-price)
  )

(defn get-current-stock-of-product-at-store
  [product-id store-id]
  (nth (nth @stock product-id) store-id))

(defn get-current-price-of-product-at-store
  [product-id store-id]
  (nth (nth @prices product-id) store-id))

(defn get-total-price [store-id product-ids-and-number]
  (reduce + (map (fn [[product-id quantity]]
                   (* quantity (get-price store-id product-id)))
                 product-ids-and-number)))

(defn get-current-stock [store-id product-id]
  (nth (nth @stock product-id) store-id))

(defn get-customer-products-with-id-and-quantity
  "returns the customer's product with product's quantity and ID"
  [customer]
  ;; it would have been nice although, if i would be able to extract
  ;; the following anon function to its own function like SWIFT
  (map (fn [[name number]] [(product-name->id name) number]) ;;(extract-product-id-and-quantity name 1)
       (:products customer)))


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

; ----------- Domain Models -------------

(defn sort-cart-by-price
  [product-id-and-quantity available-store-ids]
  (sort-by ; sort stores by total price
   (fn [store-id] (get-total-price store-id product-id-and-quantity))
   available-store-ids))

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

; ------- Business Logic -------

(def reiteration-registery (new-registry))
(def reiteration-count (counter reiteration-registery "re-iteration"))

(def customer-register (new-registry))

(defn customer-retry-counter [customer-id]
  ;; setting the id for each customer such that its easy to distinguish the results
  (counter customer-register (str "customer- " (:id customer-id))))

(defn buy-product [store-id product-id n customer-id]
  "Updates `stock` to buy `n` of the given product in the given store."
  ;; (swap! stock
  ;; using alter instead
  (dosync
  ;;  (inc! (counter reg "users-connected"))
   (inc! (counter reiteration-registery "re-iteration"))
   (inc! (customer-retry-counter customer-id))

   (let [current-stock (get-current-stock store-id product-id)]
     (when (>= current-stock n)
       (alter stock update-in [store-id product-id] - n)))))

;; 

(defn buy-products [store-id product-ids-and-number customer-id]
  (dosync
   (doseq [[product-id n] product-ids-and-number]
     (buy-product store-id product-id n customer-id))))

;; sequential processing of customer
(defn process-customer [customer]
  "Process `customer`. Consists of three steps:
  1. Finding all stores in which the requested products are still available.
  2. Sorting the found stores to find the cheapest (for the sum of all products).
  3. Buying the products by updating the `stock`.

  Note: because this implementation is sequential, we do not suffer from
  inconsistencies. That will be different in your implementation."
  (let [product-ids-and-number ;; 
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
      ;; (println "No store available for this customer " (:id customer))
      ;; (log "Customer" (:id customer) "could not find a store that has"
      ;;   (:products customer))
      ;; nil
      (do
        (buy-products cheapest-store-id product-ids-and-number customer) ;  step 3
        ;; (println "customer " (:id customer) "bought " (:products customer))
        ;; (log "Customer" (:id customer) "bought" (:products customer) "in"
        ;;   (store-id->name cheapest-store-id))
        ))))

(def finished-processing?
  "Set to true once all customers have been processed, so that sales process
  can end."
  (atom false))

;; (defn process-customers [customers]
;;   "Process `customers` one by one. In this code, this happens sequentially. In
;;   your implementation, this should be parallelized."
;;   (doseq [customer customers]
;;     (process-customer customer))
;;   (reset! finished-processing? true))

;; race condition here...
(defn start-sale [store-id]
  "Sale: -10% on `store-id`."
  ;; (log "Start sale for store" (store-id->name store-id))
  (doseq [product-id (range (count products))]
    (set-price store-id product-id (* (get-price store-id product-id) 0.90))))

(defn end-sale [store-id]
  "End sale: reverse discount on `store-id`."
  ;; (log "End sale for store" (store-id->name store-id))
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

(defn simulate-purchase-concurrent
  "Simulate the process of purchase in a concurrent environment safely."
  [customer]
  ;; (dosync
  (let [product-ids-and-number (get-customer-products-with-id-and-quantity customer)
        available-store-ids (find-available-stores product-ids-and-number)
        cheapest-store-id (when-not (empty? available-store-ids)
                            (first (sort-cart-by-price product-ids-and-number available-store-ids)))]
    (when cheapest-store-id
      (dosync  ; Ensure all operations within are transactional
       (try
         (do
          ;;  (println "bought for customer " (:id customer))
           (buy-products cheapest-store-id product-ids-and-number customer))
         (catch Exception e
          ;;  (println "Transaction failed for customer" (:id customer) ", retrying..." e)
          ;;  (recur customer)
           )))))
  (reset! finished-processing? true))  ; Optionally retry the transaction if it fails

(defn submit-task [executor-service task-fn arg]
  (.submit executor-service
           (reify Callable
             (call [this]
               (task-fn arg)))))

(defn process-customers-concurrently [customers]
  ;; adding concurrently processing the customer using map, wrapped inside future,
  ;; subsequently derefrencing it to ask for result.
  ;; equals to (fn [customer] (submit-task executor-service simulate-purchase-concurrent customer))
  (let [futures (map #(submit-task executor-service simulate-purchase-concurrent %) customers)]
    ; force the evaluation of lazy sequence
    (dorun (map deref futures))))  ; Wait for all to complete by dereferencing futures

(defn shutdown-executor []
  (.shutdown executor-service)
  (when (not (.awaitTermination executor-service 60 java.util.concurrent.TimeUnit/SECONDS))
    (.shutdownNow executor-service)))


(defn run-sim []
  (do

    ; Print parameters
    (println "Number of products:" (count products))
    (println "Number of stores:" (count stores))
    (println "Number of customers:" (count input/customers))
    (println "Time between sales:" input/TIME_BETWEEN_SALES)
    (println "Time of sales:" input/TIME_OF_SALES)

  ; Print initial stock
  ;; (println "Initial stock:")
  ;; (print-stock @stock)


    (let [f1 (future (time (process-customers-concurrently input/customers)))
          f2 (future (sales-process))]
             ; Wait until both have finished
      @f1
      @f2)

    (println "Total number of time visited: " (value reiteration-count))

    (doseq [customer input/customers]
      (println "customer: " (:id customer) ", number of time visited: " (value (customer-retry-counter customer))))

    (shutdown-executor)))

(defn -main
  [& args]
  (do
    (run-sim)
    (shutdown-agents)))

  ;; (do (println "Hello, World!")
  ;;     (main)
  ;;     (shutdown-agents)
  ;;     (foo "Hello")))

;lein run (sequential)
; "Elapsed time: 10761.226333 msecs" ;