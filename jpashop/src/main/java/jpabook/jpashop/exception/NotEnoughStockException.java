package jpabook.jpashop.exception;

public class NotEnoughStockException extends RuntimeException {

    // 이거 왜 override method 해야할까?
    // 메세지 같은거 넘겨서 이거 다 넘겨 주고, 메세지랑 이 예외가 발생한 어떤 근원적인 예외를 또 넣어서 exception trace를 알 수 있다.
    public NotEnoughStockException() {
        super();
    }

    public NotEnoughStockException(String message) {
        super(message);
    }

    public NotEnoughStockException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotEnoughStockException(Throwable cause) {
        super(cause);
    }
}
