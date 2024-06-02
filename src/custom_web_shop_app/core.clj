(ns custom-web-shop-app.core
  (:require [clojure.pprint]
            
            [custom-web-shop-app.input-random :as input]
            
            ;; [custom-web-shop-app.input-simple :as input]

            ;; [custom-web-shop-app.student-test-case :as input]
            ))

(require '[com.climate.claypoole :as cp])
(import '(java.util.concurrent Executors))

(def executor-service (Executors/newFixedThreadPool 1))

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

;; (defn print-stock [stock]
;;   "Print stock. Note: `stock` should not be an atom/ref/... but the value it
;;   contains."
;;   (println "Stock:")
;;   ; Print header row with store names (abbreviated to four characters)
;;   (doseq [store stores]
;;     (print (apply str (take 4 store)) ""))
;;   (println)
;;   ; Print table
;;   (doseq [product-id (range (count stock))]
;;     ; Line of numbers
;;     (doseq [number-in-stock (nth stock product-id)]
;;       (print (clojure.pprint/cl-format nil "~4d " number-in-stock)))
;;     ; Name of product
;;     (println (product-id->name product-id))))


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

(defn store-id->name [id]
  "Return name of store with given `id`.
  E.g. (store-id->name 0) = \"Aldi\""
  (nth stores id))

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


; ----------- Domain Models -------------

; We simply copy the products from the input file, without modifying them.
;; products are defined in input.random file (input name space)


; We wrap the stock from the input file in a single atom in this
; implementation. You are free to change this to a more appropriate mechanism.


;; (defn sort-stores-by-price [product-ids-and-number available-store-ids]
  ;; (sort-by #(get-total-price % product-ids-and-number) available-store-ids))

;; (defn get-total-price [store-id product-ids-and-number]
;;   "Returns the total price for a given number of products in the given store."
;;   (reduce +
;;     (pmap
;;       (fn [[product-id n]]
;;         (* n (get-price store-id product-id)))
;;       product-ids-and-number)))


(defn sort-cart-by-price
  [product-id-and-quantity available-store-ids]
  (sort-by #(get-total-price % product-id-and-quantity) available-store-ids))


;; (defn store-has-required-products? [store-id product-ids-and-quantity]
;;   (every? (fn [[product-id quantity]]
;;             (>= (get-current-stock store-id product-id) quantity))
;;           product-ids-and-quantity))

;; (defn find-available-stores [product-ids-and-number]
;;   (->> (range (count stores))
;;        (map (fn [store-id]
;;                (when (store-has-required-products? store-id product-ids-and-number)
;;                  store-id)))
;;        (remove nil?)
;;        (doall)))  ; Ensure completion of lazy sequence

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
    (map store-name->id stores)
   )
  )

; ------- Business Logic -------

(defn buy-product [store-id product-id n]
  "Updates `stock` to buy `n` of the given product in the given store."
  ;; (swap! stock
  ;; using alter instead
  (dosync
   (alter stock update-in [product-id store-id] #(- % n)))
  ;; (swap! stock
  ;;        (fn [old-stock]
  ;;          (update-in old-stock [product-id store-id]
  ;;                     (fn [available] (- available n)))))
  )

(defn buy-products [store-id product-ids-and-number]
  (dosync
   (doseq [[product-id n] product-ids-and-number]
     (buy-product store-id product-id n))
   )
  )

;; (defn extract-product-id-and-quantity
;;   [product-name quantity]
;;   ((product-name->id product-name) quantity))

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
      (println "No store available for this customer " (:id customer))
      ;; (log "Customer" (:id customer) "could not find a store that has"
      ;;   (:products customer))
      ;; nil
      (do
        (buy-products cheapest-store-id product-ids-and-number) ;  step 3
        (println "customer " (:id customer) "bought " (:products customer))
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
    )
  )

;; (defn simulate-purchase-concurrent
;;   "just a test code for experimental purposes for concurrency"
;;   [customer]
;;   (let [
;;         ;; getting product ids and numbers
;;         product-ids-and-number (get-customer-products-with-id-and-quantity customer)
;;         ;; product-ids-and-number (map #(vector (product-name->id (first %)) (second %))
;;         ;;                             (:products customer))

;;         ;; finding available stores using the IDs along with quantity
;;         available-store-ids (find-available-stores (get-customer-products-with-id-and-quantity customer))
;;         ;; available-store-ids (find-available-stores product-ids-and-number)

;;         ;; finding cheapest store 
;;         cheapest-store-id
;;         (if (empty? available-store-ids)
;;           nil
;;           (first  ; Returns nil if there's no available stores
;;            (sort-cart-by-price product-ids-and-number available-store-ids)))
;;         ]
    
;;     (println "cheapest store " cheapest-store-id)
    
;;     (if (= cheapest-store-id 0)
;;         (println "No store available for this customer " (:id customer))
;;         (buy-products cheapest-store-id product-ids-and-number)
;;       )
;;     )
  
;;   (reset! finished-processing? true)
;;   )

(defn simulate-purchase-concurrent
  "Process a purchase in a concurrent environment safely."
  [customer]
  (let [product-ids-and-number (get-customer-products-with-id-and-quantity customer)
        available-store-ids (find-available-stores product-ids-and-number)
        cheapest-store-id (when-not (empty? available-store-ids)
                            (first (sort-cart-by-price product-ids-and-number available-store-ids)))]
    (when cheapest-store-id
      (dosync  ; Ensure all operations within are transactional
       (try
         (do 
           (println "bought for customer " (:id customer))
           (buy-products cheapest-store-id product-ids-and-number)
           )
         (catch Exception e
           (println "Transaction failed for customer" (:id customer) ", retrying..." e)
          ;;  (recur customer)
           )))))
  (println "finishing")
  (reset! finished-processing? true)
  )  ; Optionally retry the transaction if it fails

;; (defn run-code
;;   [customers]
;;   (let [f1 (future (doseq [customer customers]
;;                      (simulate-purchase-concurrent customer)
;;                      )
;;                    )
;;         f2 (future (sales-process))]
;;               ; Wait until both have finished
;;     @f1
;;     @f2)
;;   )

;; (defn run-code
;;   [customers]
;;   (let [executor (Executors/newFixedThreadPool 12)
;;         do-work (fn [customer]
;;                   ;; Process each customer
;;                   ;; (println "Processing customer" customer)
;;                   (simulate-purchase-concurrent customer)
;;                   )]
;;     (doseq [customer customers]
;;       (.execute executor (fn [] (do-work customer))))
;;     (.shutdown executor)  ; Don't forget to shut down the executor
;;     )
;;   )

;; with futures
;; (defn run-code
;;   [customers]
;;   (let [concurrency-limit 3
;;         queue (java.util.concurrent.LinkedBlockingQueue. concurrency-limit)]
;;     (doseq [customer customers]
;;       (.put queue (future (process-customer customer))))
;;     (dorun (map deref (take (.size queue) (repeatedly #(.take queue)))))))

(defn submit-task [executor-service task-fn arg]
  (.submit executor-service
           (reify Callable
             (call [this]
               (task-fn arg)))))

(defn process-customers-concurrently [customers]
  (let [futures (map #(submit-task executor-service simulate-purchase-concurrent %) customers)]
    (dorun (map deref futures))))  ; Wait for all to complete by dereferencing futures

(defn shutdown-executor []
  (.shutdown executor-service)
  (when (not (.awaitTermination executor-service 60 java.util.concurrent.TimeUnit/SECONDS))
    (.shutdownNow executor-service)))


(defn run-sim []
  (do

    ;; (time (cp/pmap 1000 run-code input/customers))

    ;; (time (run-code input/customers))

    (time (process-customers-concurrently input/customers))
    (shutdown-executor)

    ;;  (time (process-customers-concurrently input/customers))
        ;; HUGE INPUT FOR CUSTOMER BASE
        ;; (time
        ;; (doseq [customer input/customers]
        ;;   (simulate-purchase customer)
        ;;   )
        ;; )

        ;; (time (process-customers input/customers))
        ;; (time (run-code input/customers))

        ;; HUGE INPUT FOR CUSTOMER BASE
        ;; (time
        ;; (doseq [customer input/customers]
        ;;   (simulate-purchase customer)
        ;;   )
        ;; )

    ;; (time
    ;;  (doseq [customer input/customers]
    ;;         (simulate-purchase-concurrent customer)
    ;;    )
    ;;  )

        ;; (time
        ;;  (let [futures (ref [])]
        ;;    (doseq [customer input/customers]
        ;;      (let [f (future (simulate-purchase-concurrent customer))]
        ;;        (dosync (alter futures conj f))))
        ;;    (dorun (map deref @futures))
        ;;    )
        ;;  )

        ; Print the time to execute the first thread.
        ;; (let [f1 (future (time (process-customers input/customers)))
        ;;       f2 (future (sales-process))]
        ;;   ; Wait until both have finished
        ;;   @f1
        ;;   @f2
        ;;   (await logger)
        ;;   )

        ;; (time
        ;;  (doseq [customer input/customers]

        ;;    (let [f (future (simulate-purchase-concurrent customer))]
        ;;      (deref f))
        ;;   ;; (await logger)
        ;;    )
        ;;   )

        ;; (let [f1 (future (time (process-customers input/customers)))
        ;;       f2 (future (sales-process))]
        ;;     ; Wait until both have finished
        ;;   @f1
        ;;   @f2
        ;;   )
    ))

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

;; (defn run-test [thread-count]
;;   (time
;;   ;;  (pmap-limited thread-count process-customers input/customers)

;;    (pmap-limited thread-count simulate-purchase-concurrent input/customers)

;;   ;;  (let [f1 (future (time (doseq [customer input/customers]
;;   ;;                           (simulate-purchase-concurrent customer))
;;   ;;                                  ;;  (let [f (future (simulate-purchase-concurrent customer))]
;;   ;;                                  ;;    (deref f)) 
;;   ;;                                  ;;  )
;;   ;;                         ))
;;   ;;        f2 (future (sales-process))]
;;   ;;                      ; Wait until both have finished
;;   ;;    @f1
;;   ;;    @f2)
;;    )
;;   )


;; (run-test 5)   ; Run with 5 threads
;; (run-test 10)  ; Run with 10 threads
;; (run-test 20)  ; Run with 20 threads



(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (do
    ;; (main)
    (run-sim)
    ;; (run-test 1)
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