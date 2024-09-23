(ns custom-web-shop-app.core
  (:require [clojure.pprint]

            [custom-web-shop-app.insufficient-store-test-case :as input]            

            ;; [custom-web-shop-app.input-random :as input]

            ;; [custom-web-shop-app.input-simple :as input]

            ;; [custom-web-shop-app.student-test-case :as input]
            ))

(import '(java.util.concurrent Executors))
(require '[metrics.core :refer [new-registry]])
(require '[metrics.counters :refer [counter]])
(require '[metrics.counters :refer [inc!]])

; ------- Misc --------

;; https://stackoverflow.com/questions/5397955/sleeping-a-thread-inside-an-executorservice-java-clojure
;; https://stackoverflow.com/questions/41284533/concurrent-process-in-clojure
;; https://github.com/clj-commons/claypoole
(def executor-service (Executors/newFixedThreadPool 1))

(def reiteration-registery (new-registry))
(def reiteration-count (counter reiteration-registery "re-iteration"))

(def customer-register (new-registry))

(defn customer-retry-counter [customer-id]
  ;; setting the id for each customer such that its easy to distinguish the results
  (counter customer-register (str "customer- " (:id customer-id))))

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

;; (def stock (atom nil))
;; (def prices (atom nil))

(def products input/products)
(def stores input/stores)

;; (def prices (ref input/prices))
;; (def prices (atom input/prices))

;; (def stock (atom (mapv ref input/stock)))
;; (def prices (atom (mapv ref input/prices)))

;; the following combination 
;; (storing entire matrix in Atom with each item in ref)
;; allows the overall collection
;; to be updated independently (using atoms)
;; while ensuring the individual store's price
;; changes are transactional and consistent (using refs).

(def prices
  (atom
   (vec
    (for [store-prices input/prices]
      (vec (map ref store-prices))))))

; Initialize stock atom with refs
(def stock
  (atom
   (vec
    (for [store-stock input/stock]
      (vec (map ref store-stock))))))


(defn product-name->id [name]
  "Return id (= index) of product with given `name`.
  E.g. (product-name->id \"Apple\") = 0"
  (.indexOf products name))

;; add a condition where the index doesn't fall below -1!! (it goes for every index (int))
(defn store-name->id [name]
  "Return id (= index) of store with given `name`.
  E.g. (store-name->id \"Aldi\") = 0"
  (.indexOf stores name))

;; https://stackoverflow.com/questions/8087115/clojure-index-of-a-value-in-a-list-or-other-collection

(defn index-of [coll item]
  (first (keep-indexed (fn [idx val] (when (= val item) idx)) coll)))

(defn get-price [product store]
  (let [store-index (index-of input/stores store)]
    (if-let [price-ref (get-in @prices [product store-index])]
      (@price-ref)
      0)))

(defn set-price [store-id product-id new-price]
  "Set the price of the given product in the given store to `new-price`."
  (swap! prices assoc-in [product-id store-id] new-price)
  ;; (dosync alter @prices assoc-in [product-id store-id] new-price)
  )

(defn get-total-price [store-id product-ids-and-number]
  (reduce + (map (fn [[product-id quantity]]
                   (* quantity (get-price store-id product-id)))
                 product-ids-and-number)))

(defn get-customer-products-with-id-and-quantity
  "returns the customer's product with product's quantity and ID"
  [customer]
  ;; it would have been nice although, if i would be able to extract
  ;; the following anon function to its own function like SWIFT
  (map (fn [[name number]] [(product-name->id name) number]) ;;(extract-product-id-and-quantity name 1)
       (:products customer)))

; ----------- Domain Models -------------

(defn sort-cart-by-price
  [product-id-and-quantity available-store-ids]
  (sort-by ; sort stores by total price
   (fn [store-id] (get-total-price store-id product-id-and-quantity))
   available-store-ids))

;; (defn product-available? [store-id product-id n]
;;   "Returns true if at least `n` of the given product are still available in the
;;   given store."
;;   (>= (nth (nth @stock product-id) store-id) n))

;; (defn find-available-stores [product-ids-and-number]
;;   "Returns the id's of the stores in which the given products are still
;;   available."
;;   (filter
;;    (fn [store-id]
;;      (every?
;;       (fn [[product-id n]] (product-available? store-id product-id n))
;;       product-ids-and-number))
;;    (map store-name->id stores)))

(defn get-stock [product store]
  (let [store-index (index-of input/stores store)]
    (if-let [stock-ref (get-in @stock [product store-index])]
      (do
        ;; (log "get-stock:" product store "index:" store-index "stock-ref:" @stock-ref)
        (if (nil? @stock-ref) 0 @stock-ref))
      (do
        ;; (log "get-stock: stock-ref is nil for" product store "index:" store-index)
        0))))

;; find the available stores for the given product quantity and ids
;; product-ids-and-number: list of tuple [id-N quantity]
(defn find-available-stores [product-ids-and-number]
  (filter (fn [store-id]
            (every? (fn [[product-id number]]
                      (let [stock (get-stock product-id (nth input/stores store-id))]
                        ;; (log "Checking stock for product" product-id "in store" store-id "required:" number "available:" stock)
                        ;; adding a condition because without it the stock falls below -1
                        (>= stock number)))
                    product-ids-and-number))
          (range (count input/stores))))

; ------- Business Logic -------

(defn buy-products [store-id product-ids-and-number customer]
  (dosync
   (doseq [[product-id number] product-ids-and-number]
    ;;  (let [current-stock (get-stock product-id (nth stores store-id))]
        ;; (print "\ncurrent-stock" current-stock)
        ;; (print "")

      ;; (if (>= current-stock number)
       
        (inc! (counter reiteration-registery "re-iteration"))
        (inc! (customer-retry-counter customer))
        (alter (get-in @stock [product-id store-id]) - number))))

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
  
  (let [product-ids-and-number (get-customer-products-with-id-and-quantity customer)
        available-store-ids (find-available-stores product-ids-and-number)
        cheapest-store-id (when-not (empty? available-store-ids)
                            (first (sort-cart-by-price product-ids-and-number available-store-ids)))]
    (when cheapest-store-id
      ;; (dosync  ; Ensure all operations within are transaction otherwise it leads to negative values
       (try
         (do
          ;;  (println "bought for customer " (:id customer))
          ;;  (buy-products cheapest-store-id product-ids-and-number customer)
           (buy-products cheapest-store-id product-ids-and-number customer))
        ;;  (catch Exception e
        ;;    (println "Transaction failed for customer" (:id customer) ", retrying..." e)
        ;;   ;;  (recur customer)
        ;;    )
         ))))
  (reset! finished-processing? true)
    ; Optionally retry the transaction if it fails

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

(defn product-id->name [id]
  "Return name of product with given `id`.
  E.g. (product-id->name 0) = \"Apple\""
  (nth products id))

(defn print-stock []
  "Print stock from the refs."
  (println "\nStock:")
  (doseq [store stores]
    (print (apply str (take 4 store)) " "))
  (println)
  (doseq [product-id (range (count @stock))]
    (doseq [store-id (range (count (nth @stock product-id)))]
      (print (clojure.pprint/cl-format nil "~4d " @(get-in @stock [product-id store-id]))))
    (println (product-id->name product-id))
    (println)))

;; in case of locks,
;; A mutex (lock) would be associated with the entire stock and price matrices.
;; would allow only one thread per transaction (performance bottleneck)
;; hard to manage since they can introduce deadlocks too if not managed properly


(defn run-sim []
  (do
    ; Print parameters
    ;; (println "Number of products:" (count products))
    ;; (println "Number of stores:" (count stores))
    ;; (println "Number of customers:" (count input/customers))
    ;; (println "Time between sales:" input/TIME_BETWEEN_SALES)
    ;; (println "Time of sales:" input/TIME_OF_SALES)

    ;Print initial stock
    (println "Initial stock:")
    (print-stock)

    (let [f1 (future (time (process-customers-concurrently input/customers)))
          f2 (future (sales-process))]
             ; Wait until both have finished
      @f1
      @f2)

    ;; (print-stock)
    ;Print initial stock
    (println "Final stock:")
    (print-stock)
    ;; (println "Total number of time visited: " (value reiteration-count))

    ;; (doseq [customer input/customers]
    ;;   (println "customer: " (:id customer) ", number of time visited: " (value (customer-retry-counter customer))))

    (shutdown-executor)))

(defn -main
  [& args]
  (do
    (run-sim)
    (shutdown-agents)))