public class User {
    private String username;
    private String password;
    private String role;


    public User(
            String username,String password,String role
    ){

        this.password=password;
        this.username=username;
        this.role=role;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }
}
