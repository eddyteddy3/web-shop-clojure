(ns custom-web-shop-app.core-test
  (:require [clojure.test :refer :all]
            [custom-web-shop-app.core :refer :all]
            [custom-web-shop-app.input-simple :as input]))

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

(deftest test-buy-product
  (is (= 15 (get-current-stock-of-product-at-store 0 0))) ; stock of apple at Aldi [0, 0]
  (do (buy-product 0 0 5) ; Buy 5 Apples at aldi
      (is (= 10 (get-current-stock-of-product-at-store 0 0)))))

(def mock-stock [[15 25 30 29 15]
                 [5 7 6 10 2]
                 [2 10 20 17 8]
                 [25 17 31 18 20]])

(def mock-stock-for-test-with-atom (atom mock-stock))
;; (def- moc "hello")
;; (deftest test-process-customer
;;   (testing "Processing a customer"
;;     ;; temporarily binding the value of mock stock with actual stock
;;     (with-redefs [stock mock-stock-for-test-with-atom]
;;       (is (= 15 (get-current-stock-of-product-at-store 0 0))) ; Stock of Apple at Aldi before purchase
;;       (is (= 2 (get-current-stock-of-product-at-store 2 0))) ; Stock of Banana at Aldi before purchase
;;       (process-customer {:id 1 :products [["Apple" 2] ["Banana" 1]]})
;;       (is (= 13 (get-current-stock-of-product-at-store 0 0))) ; Stock of Apple at Aldi after purchase
;;       (is (= 1 (get-current-stock-of-product-at-store 2 0)))) ; Stock of Banana at Aldi after purchase
;;     )) ; Stock of Banana at Aldi after purchase

(reset! mock-stock-for-test-with-atom mock-stock)

(def mock-customer-1 {:id 0 :products [["Apple" 5]]})
(def mock-customer-2 (nth input/customers 1))
(def mock-customer-3 (nth input/customers 2))

(def mock-customers [mock-customer-1 mock-customer-2 mock-customer-3])

;; idk if that is the right noun for sequential :P 
(deftest test-sequentialism
  (with-redefs [stock mock-stock-for-test-with-atom]  ; Redefine stock only within this test
    (is (= 15 (get-current-stock-of-product-at-store 0 0))) ;; checking the stock of apple
    (process-customer (nth mock-customers 0))
    (is (= 10 (get-current-stock-of-product-at-store 0 0))) ;; checking the stock of apple

    (is (= 20 (get-current-stock-of-product-at-store 2 2)))
    (process-customer (nth mock-customers 1))
    (is (= 13 (get-current-stock-of-product-at-store 2 2)))

    (reset! mock-stock-for-test-with-atom mock-stock)
    (is (= 15 (get-current-stock-of-product-at-store 0 0)))
    (is (= 20 (get-current-stock-of-product-at-store 2 2)))


    (process-customers mock-customers)

    (is (= 5 (get-current-stock-of-product-at-store 3 4)))
    ;; (is (= 15 (get-current-stock-of-product-at-store 0 0)))
    ;; (process-customer (nth mock-customers 2))
    ;; (future (process-customer {:id 1 :products [["Apple" 5]]}))
    ;; (future (process-customer {:id 2 :products [["Apple" 10]]}))
    ;; (await)
    ;; (println "nth stock 0" (nth @stock 0))
    ;; (println "nth " (nth (nth @stock 0) 0))
    ;; (is (<= (nth (nth @stock 0) 0) 0)))
    ) ; Checking stock shouldn't be negative
  )
 ; Checking stock shouldn't be negative

(deftest test-sale
  (is (= 0.18 (get-current-price-of-product-at-store 3 4))) ; stock of pear at Lidl [3, 4]
  (do (start-sale 4) ; Buy 5 Apples at aldi 
      (is (= 0.162 (get-current-price-of-product-at-store 3 4)))
      (end-sale 4)
      (is (= 0.18 (get-current-price-of-product-at-store 3 4))))
  )

;; (deftest test-cheapest-store-after-sale
;;   (do
;;     ))

;; (deftest test-product-and-store-ids
;;   (testing "product-name->id and store-name->id"
;;     (let [test-products ["Apple", "Avocado", "Banana", "Pear"]
;;           test-stores ["Aldi", "Carrefour", "Colruyt", "Delhaize", "Lidl"]]
;;       (with-redefs [products test-products
;;                     stores test-stores]
;;         (is (= 0 (product-name->id "Apple")))
;;         (is (= 1 (store-name->id "Carrefour")))
;;         (is (= 3 (product-name->id "Pear")))
;;         (is (= -1 (product-name->id "Mango"))))))) ;; why -1 but not nil?

;; (deftest test-get-total-price
;;   (with-redefs [get-price (fn [store-id product-id] (if (= product-id 1) 1.20 1.00))]
;;     (is (= 5.6 (get-total-price 1 [[0 2] [1 3]])))))


;; (deftest test-price-calculation
;;   (testing "get-price and get-total-price"
;;     (let [test-prices (atom [[1.00 0.50 2.50] [0.90 1.20 3.00]]) ; Correct structure for two stores
;;           test-store-id 1  ; Should reference the second store
;;           test-product-ids-and-number [[0 2] [1 3]]]  ; 2 units of product 0, 3 units of product 1
;;       (with-redefs [prices test-prices]  ; Ensure the redefined atom is used
;;         (is (= 1.20 (get-price test-store-id 1)))  ; Checking price of product ID 1 at store ID 1
;;         (is (= 4.6 (get-total-price test-store-id test-product-ids-and-number))))))) ;; should be 5.6

;; (deftest test-stock-management
;;     (testing "buy-product updates stock correctly"
;;       (let [initial-stock [[15 25 30 29 15]  ; Stock for Apple
;;                            [5 7 6 10 2]     ; Stock for Avocado
;;                            [2 10 20 17 8]   ; Stock for Banana
;;                            [25 17 31 18 20]] ; Stock for Pear
;;             test-store-id 0  ; Aldi
;;             test-product-id 0  ; Apple
;;             number-to-buy 5]
;;         (with-redefs [stock (atom initial-stock)]
;;           (buy-product test-store-id test-product-id number-to-buy)
;;           (is (= 10 (nth (nth @stock test-product-id) test-store-id)))))))  ;; Check remaining stock of Apple at Aldi