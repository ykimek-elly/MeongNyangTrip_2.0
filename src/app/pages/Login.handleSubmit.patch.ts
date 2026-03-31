// Login.tsx handleSubmit 부분만 수정 — 나머지 코드는 기존과 동일

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password.trim()) return setError('이메일과 비밀번호를 입력해주세요.');
    setError('');
    setIsLoading(true);
    try {
      const res = await authApi.login(email, password);
      localStorage.setItem('accessToken', res.token);
      localStorage.setItem('refreshToken', res.refreshToken); // 신규
      login(res.nickname, res.email, res.userId, res.profileImage, res.role === 'ADMIN');
      onNavigate('home');
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.');
    } finally {
      setIsLoading(false);
    }
  };
