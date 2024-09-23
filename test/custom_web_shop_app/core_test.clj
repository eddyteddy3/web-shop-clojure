(ns custom-web-shop-app.core-test
  (:require [clojure.test :refer :all]
            [custom-web-shop-app.core :refer :all]
            [custom-web-shop-app.input-simple :as input]
            ;; [custom-web-shop-app.student-test-case :as input]
            ))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 1 1))))

(deftest test-sample
  (testing "Test product id for its name"
    (is (= 0 (product-name->id "Apple")))))

(deftest test-product-name-to-id
  (is (= 0 (product-name->id "Apple")))
  (is (= 1 (product-name->id "Avocado")))
  (is (= 2 (product-name->id "Banana")))
  (is (= 3 (product-name->id "Pear"))))

(deftest test-store-name-to-id
  (is (= 0 (store-name->id "Aldi")))
  (is (= 1 (store-name->id "Carrefour")))
  (is (= 2 (store-name->id "Colruyt")))
  (is (= 3 (store-name->id "Delhaize")))
  (is (= 4 (store-name->id "Lidl"))))

(deftest test-get-and-set-price
  (is (= 0.25 (get-price 0 0))) ; Price of Apple at Aldi [0, 0]
  (do (set-price 0 0 0.20) ; Set new price
      (is (= 0.20 (get-price 0 0)))))

;; (deftest test-buy-product
;;   (is (= 15 (get-current-stock-of-product-at-store 0 0))) ; stock of apple at Aldi [0, 0]
;;   (do (buy-product 0 0 5 0) ; Buy 5 Apples at aldi
;;       (is (= 10 (get-current-stock-of-product-at-store 0 0)))))

;; ;; (def- moc "hello")

(def mock-customer-1 {:id 0 :products [["Apple" 5]]})
(def mock-customer-2 (nth input/customers 1))
(def mock-customer-3 (nth input/customers 2))

(def mock-customers [mock-customer-1 mock-customer-2 mock-customer-3])

(def mock-stock [[15 25 30 29 15]
                 [5 7 6 10 2]
                 [2 10 20 17 8]
                 [25 17 31 18 20]])

;; (def mock-stock-for-test-with-ref (ref mock-stock))
;; (dosync (ref-set stock mock-stock-for-test-with-ref))

;; ;; idk if that is the right noun for sequential :P 
;; (deftest test-sequentialism
;;   (dosync
;;    (ref-set stock mock-stock)
;;    (is (= 15 (get-current-stock-of-product-at-store 0 0))) ;; checking the stock of apple
;;    (process-customer (nth mock-customers 0))
;;    (is (= 10 (get-current-stock-of-product-at-store 0 0))) ;; checking the stock of apple
   
;;    (is (= 20 (get-current-stock-of-product-at-store 2 2)))
;;    (process-customer (nth mock-customers 1))
;;    (is (= 13 (get-current-stock-of-product-at-store 2 2)))
   
;;    (ref-set stock mock-stock)
;;    (is (= 15 (get-current-stock-of-product-at-store 0 0)))
;;    (is (= 20 (get-current-stock-of-product-at-store 2 2)))
   
;;    (process-customers mock-customers)
   
;;    (is (= 5 (get-current-stock-of-product-at-store 3 4)))
;;    )
;;   )

;; (deftest test-sale
;;   (is (= 0.18 (get-current-price-of-product-at-store 3 4))) ; stock of pear at Lidl [3, 4]
;;   (do (start-sale 4) ; Buy 5 Apples at aldi 
;;       (is (= 0.162 (get-current-price-of-product-at-store 3 4)))
;;       (end-sale 4)
;;       (is (= 0.18 (get-current-price-of-product-at-store 3 4))))
;;   )

(deftest test-concurrent-purchasing
  (dosync
   (ref-set stock mock-stock)
   (doseq [customer mock-customers]
     (simulate-purchase-concurrent customer))
       ;; Optional: add a slight delay here to let some futures complete
   (Thread/sleep 2000)
       ;; Assertions can check specific stock levels here
   (is (= (get-current-stock 0 0) 10))
   ))

;; ;; ----------------------------- Low priority

(deftest test-product-purchase-under-low-stock
  ;; (reset-environment)
  ;; Set up the environment with low stock for a certain product
  (dosync
   (ref-set stock (vec (repeat (count input/products) (vec (repeat (count stores) 1)))))
   
   (let [customer {:id 1 :products [["Apple" 2]]}  ; Customer wants 2 Apples, but only 1 is available
         store-id (store-name->id "Aldi")
         product-id (product-name->id "Apple")]
     (future (process-customer customer))
       ;; Allow some time for the transaction to process
     (Thread/sleep 1000)
       ;; Check if the stock level was not allowed to go negative
    ;;  (is (= 0 (get-current-stock store-id product-id)))
       ;; Check if the transaction was aborted or handled gracefully
     (is (not (:purchase-successful? customer))))
   )
  )