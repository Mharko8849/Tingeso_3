const Logo = () => {
  const goHome = () => {
    // use hash routing to go to home
    globalThis.location.href = '/';
  };

  return (
    <button
      type="button"
      className="logo"
      onClick={goHome}
      style={{ cursor: 'pointer', fontWeight: 'bold', fontSize: '1.4rem', color: '#2c3e50', background: 'none', border: 'none', padding: 0 }}
    >
      ToolRent
    </button>
  );
};

export default Logo;
